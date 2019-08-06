/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */

package com.terracottatech.dynamic_config.nomad;

import com.terracottatech.diagnostic.common.DiagnosticConstants;
import com.terracottatech.diagnostic.server.DiagnosticServices;
import com.terracottatech.diagnostic.server.DiagnosticServicesRegistration;
import com.terracottatech.dynamic_config.DynamicConfigConstants;
import com.terracottatech.dynamic_config.nomad.exception.NomadConfigurationException;
import com.terracottatech.dynamic_config.nomad.processor.ApplicabilityNomadChangeProcessor;
import com.terracottatech.dynamic_config.nomad.processor.ClusterActivationNomadChangeProcessor;
import com.terracottatech.dynamic_config.nomad.processor.RoutingNomadChangeProcessor;
import com.terracottatech.dynamic_config.nomad.processor.SettingNomadChangeProcessor;
import com.terracottatech.dynamic_config.repository.NomadRepositoryManager;
import com.terracottatech.nomad.NomadEnvironment;
import com.terracottatech.nomad.messages.AcceptRejectResponse;
import com.terracottatech.nomad.messages.ChangeDetails;
import com.terracottatech.nomad.messages.CommitMessage;
import com.terracottatech.nomad.messages.DiscoverResponse;
import com.terracottatech.nomad.messages.PrepareMessage;
import com.terracottatech.nomad.server.ChangeApplicator;
import com.terracottatech.nomad.server.NomadException;
import com.terracottatech.nomad.server.NomadServer;
import com.terracottatech.nomad.server.SingleThreadedNomadServer;
import com.terracottatech.nomad.server.UpgradableNomadServer;
import com.terracottatech.persistence.sanskrit.SanskritException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.terracottatech.dynamic_config.DynamicConfigConstants.LICENSE_FILE_NAME;
import static com.terracottatech.dynamic_config.util.ParameterSubstitutor.substitute;
import static java.util.Objects.requireNonNull;

public class NomadBootstrapper {
  private static final Logger LOGGER = LoggerFactory.getLogger(NomadBootstrapper.class);
  private static volatile NomadServerManager nomadServerManager;
  private static volatile NomadRepositoryManager nomadRepositoryManager;
  private static final AtomicBoolean BOOTSTRAPPED = new AtomicBoolean();

  public static void bootstrap(Path nomadRoot, String nodeName) {
    requireNonNull(nomadRoot);
    requireNonNull(nodeName);

    if (BOOTSTRAPPED.compareAndSet(false, true)) {
      nomadServerManager = new NomadServerManager();
      nomadRepositoryManager = nomadServerManager.init(nomadRoot, nodeName);
      DynamicConfigConstants.setLicensePath(nomadRepositoryManager.getLicensePath().resolve(LICENSE_FILE_NAME));
      LOGGER.info("Bootstrapped nomad system with root: {}", substitute(nomadRoot));
    }
  }

  public static NomadServerManager getNomadServerManager() {
    return nomadServerManager;
  }

  public static NomadRepositoryManager getNomadRepositoryManager() {
    return nomadRepositoryManager;
  }

  public static class NomadServerManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(NomadServerManager.class);

    private volatile UpgradableNomadServer<String> nomadServer;

    public UpgradableNomadServer<String> getNomadServer() {
      return nomadServer;
    }

    private final NomadEnvironment nomadEnvironment = new NomadEnvironment();

    /**
     * Initializes the Nomad system
     *
     * @param nomadRoot Configuration repository root
     * @param nodeName  Node name
     * @throws NomadConfigurationException if initialization of underlying server fails.
     */
    NomadRepositoryManager init(Path nomadRoot, String nodeName) throws NomadConfigurationException {
      try {
        NomadRepositoryManager repositoryManager = createNomadRepositoryManager(nomadRoot);
        repositoryManager.createDirectories();
        nomadServer = createServer(repositoryManager, nodeName);
        registerDiagnosticService();
        LOGGER.info("Successfully initialized NomadServerManager");
        return repositoryManager;
      } catch (Exception e) {
        throw new NomadConfigurationException("Exception initializing Nomad Server: " + e.getMessage(), e);
      }
    }

    @SuppressWarnings("unchecked")
    void registerDiagnosticService() {
      DiagnosticServicesRegistration<NomadServer<String>> registration = (DiagnosticServicesRegistration<NomadServer<String>>) (DiagnosticServicesRegistration) DiagnosticServices.register(NomadServer.class, nomadServer);
      registration.registerMBean(DiagnosticConstants.MBEAN_NOMAD);
    }

    /**
     * Makes Nomad server capable of write operations.
     *
     * @param nodeName Name of the running node, non-null
     * @param stripeId ID of the stripe where the node belongs, should be greater than 1
     */
    public void upgradeForWrite(String nodeName, int stripeId) {
      ConfigController configController = createConfigController(nodeName, stripeId);
      RoutingNomadChangeProcessor nomadChangeProcessor = new RoutingNomadChangeProcessor()
          .register(SettingNomadChange.class, SettingNomadChangeProcessor.get())
          .register(ClusterActivationNomadChange.class, new ClusterActivationNomadChangeProcessor(configController));

      ChangeApplicator<String> changeApplicator = new ConfigChangeApplicator(new ApplicabilityNomadChangeProcessor(configController, nomadChangeProcessor));
      nomadServer.setChangeApplicator(changeApplicator);
      LOGGER.info("Successfully completed upgradeForWrite procedure");
    }

    /**
     * Used for getting the configuration string from Nomad
     *
     * @return Stored configuration as a String
     * @throws NomadConfigurationException if configuration is unavailable or corrupted
     */
    public String getConfiguration() throws NomadConfigurationException {
      ChangeDetails<String> latestChange;
      try {
        latestChange = nomadServer.discover().getLatestChange();
      } catch (NomadException e) {
        String errorMessage = "Exception while making discover call to Nomad";
        throw new NomadConfigurationException(errorMessage, e);
      }

      String missingConfigErrorMessage = "Did not get last stored configuration from Nomad";
      if (latestChange == null) {
        throw new NomadConfigurationException(missingConfigErrorMessage);
      }

      String configuration = latestChange.getResult();
      if (configuration == null || configuration.isEmpty()) {
        throw new NomadConfigurationException(missingConfigErrorMessage);
      }
      return configuration;
    }

    /**
     * Used for overriding corrupted repository content
     *
     * @param configuration Configuration string which will override the existing configuration (if any)
     * @param version       Version of the configuration. This can be used to keep the configuration version in sync with other
     *                      servers.
     * @throws NomadConfigurationException if underlying server fails to override corrupted repository content
     */
    public void repairConfiguration(String configuration, long version) throws NomadConfigurationException {
      try {
        UUID nomadRequestId = UUID.randomUUID();
        DiscoverResponse<String> discoverResponse = nomadServer.discover();
        long mutativeMessageCount = discoverResponse.getMutativeMessageCount();
        PrepareMessage prepareMessage = new PrepareMessage(mutativeMessageCount, getHost(), getUser(), nomadRequestId,
            version, new ConfigRepairNomadChange(configuration));

        AcceptRejectResponse response = nomadServer.prepare(prepareMessage);
        if (!response.isAccepted()) {
          throw new NomadConfigurationException("Repair message is rejected by Nomad. Reason for rejection is "
              + response.getRejectionReason().name());
        }
        long nextMutativeMessageCount = mutativeMessageCount + 1;
        CommitMessage commitMessage = new CommitMessage(nextMutativeMessageCount, getHost(), getUser(), nomadRequestId);
        AcceptRejectResponse commitResponse = nomadServer.commit(commitMessage);
        if (!commitResponse.isAccepted()) {
          throw new NomadConfigurationException("Unexpected commit failure. Reason for failure is "
              + commitResponse.getRejectionReason().name());
        }
      } catch (NomadException e) {
        throw new NomadConfigurationException("Unable to repair configuration", e);
      }
    }

    UpgradableNomadServer<String> createServer(NomadRepositoryManager repositoryManager, String nodeName) throws SanskritException, NomadException {
      return new SingleThreadedNomadServer<>(UpgradableNomadServerFactory.createServer(repositoryManager, null, nodeName));
    }

    NomadRepositoryManager createNomadRepositoryManager(Path nomadRoot) {
      return new NomadRepositoryManager(nomadRoot);
    }

    ConfigController createConfigController(String nodeName, int stripeId) {
      requireNonNull(nodeName);
      if (stripeId < 1) {
        throw new IllegalArgumentException("Stripe ID should be greater than or equal to 1");
      }

      return new ConfigControllerImpl(() -> nodeName, () -> stripeId);
    }

    String getHost() {
      return nomadEnvironment.getHost();
    }

    String getUser() {
      return nomadEnvironment.getUser();
    }
  }
}
