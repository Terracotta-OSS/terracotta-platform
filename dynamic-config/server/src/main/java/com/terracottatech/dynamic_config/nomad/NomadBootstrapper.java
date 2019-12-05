/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */

package com.terracottatech.dynamic_config.nomad;

import com.terracottatech.diagnostic.common.DiagnosticConstants;
import com.terracottatech.diagnostic.server.DiagnosticServices;
import com.terracottatech.diagnostic.server.DiagnosticServicesRegistration;
import com.terracottatech.dynamic_config.diagnostic.DynamicConfigService;
import com.terracottatech.dynamic_config.diagnostic.TopologyService;
import com.terracottatech.dynamic_config.handler.ConfigChangeHandlerManager;
import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.Node;
import com.terracottatech.dynamic_config.model.NodeContext;
import com.terracottatech.dynamic_config.model.Setting;
import com.terracottatech.dynamic_config.nomad.processor.ApplicabilityNomadChangeProcessor;
import com.terracottatech.dynamic_config.nomad.processor.ClusterActivationNomadChangeProcessor;
import com.terracottatech.dynamic_config.nomad.processor.RoutingNomadChangeProcessor;
import com.terracottatech.dynamic_config.nomad.processor.SettingNomadChangeProcessor;
import com.terracottatech.dynamic_config.repository.NomadRepositoryManager;
import com.terracottatech.dynamic_config.service.DynamicConfigEventing;
import com.terracottatech.dynamic_config.service.DynamicConfigServiceImpl;
import com.terracottatech.dynamic_config.util.IParameterSubstitutor;
import com.terracottatech.nomad.NomadEnvironment;
import com.terracottatech.nomad.messages.AcceptRejectResponse;
import com.terracottatech.nomad.messages.ChangeDetails;
import com.terracottatech.nomad.messages.CommitMessage;
import com.terracottatech.nomad.messages.DiscoverResponse;
import com.terracottatech.nomad.messages.PrepareMessage;
import com.terracottatech.nomad.server.NomadException;
import com.terracottatech.nomad.server.NomadServer;
import com.terracottatech.nomad.server.UpgradableNomadServer;
import com.terracottatech.persistence.sanskrit.SanskritException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import static java.util.Objects.requireNonNull;

public class NomadBootstrapper {
  private static final Logger LOGGER = LoggerFactory.getLogger(NomadBootstrapper.class);
  private static volatile NomadServerManager nomadServerManager;
  private static final AtomicBoolean BOOTSTRAPPED = new AtomicBoolean();

  public static NomadServerManager bootstrap(Path repositoryPath, IParameterSubstitutor parameterSubstitutor, ConfigChangeHandlerManager manager, String nodeName) {
    requireNonNull(repositoryPath);
    requireNonNull(nodeName);
    requireNonNull(parameterSubstitutor);
    requireNonNull(manager);

    if (BOOTSTRAPPED.compareAndSet(false, true)) {
      nomadServerManager = new NomadServerManager();
      nomadServerManager.init(repositoryPath, parameterSubstitutor, manager, nodeName);
      LOGGER.info("Bootstrapped nomad system with root: {}", parameterSubstitutor.substitute(repositoryPath.toString()));
    }

    return nomadServerManager;
  }

  public static NomadServerManager bootstrap(Path repositoryPath, IParameterSubstitutor parameterSubstitutor, ConfigChangeHandlerManager manager, NodeContext nodeContext) {
    requireNonNull(repositoryPath);
    requireNonNull(nodeContext);
    requireNonNull(parameterSubstitutor);
    requireNonNull(manager);

    if (BOOTSTRAPPED.compareAndSet(false, true)) {
      nomadServerManager = new NomadServerManager();
      nomadServerManager.init(repositoryPath, parameterSubstitutor, manager, nodeContext);
      LOGGER.info("Bootstrapped nomad system with root: {}", parameterSubstitutor.substitute(repositoryPath.toString()));
    }

    return nomadServerManager;
  }

  public static NomadServerManager getNomadServerManager() {
    return nomadServerManager;
  }

  public static class NomadServerManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(NomadServerManager.class);

    private volatile UpgradableNomadServer<NodeContext> nomadServer;
    private volatile NomadRepositoryManager repositoryManager;
    private volatile ConfigChangeHandlerManager configChangeHandlerManager;
    private volatile IParameterSubstitutor parameterSubstitutor;
    private volatile DynamicConfigServiceImpl dynamicConfigService;

    public UpgradableNomadServer<NodeContext> getNomadServer() {
      return nomadServer;
    }

    private final NomadEnvironment nomadEnvironment = new NomadEnvironment();

    /**
     * Initializes the Nomad system
     *
     * @param repositoryPath       Configuration repository root
     * @param parameterSubstitutor parameter substitutor
     * @param nodeName             Node name
     * @throws NomadConfigurationException if initialization of underlying server fails.
     */
    private void init(Path repositoryPath, IParameterSubstitutor parameterSubstitutor, ConfigChangeHandlerManager manager, String nodeName) throws NomadConfigurationException {
      try {
        this.parameterSubstitutor = parameterSubstitutor;
        this.configChangeHandlerManager = manager;
        this.repositoryManager = createNomadRepositoryManager(repositoryPath, parameterSubstitutor);
        this.repositoryManager.createDirectories();
        this.nomadServer = createServer(repositoryManager, nodeName, parameterSubstitutor, (version, updatedNodeContext) -> dynamicConfigService.newTopologyCommitted(version, updatedNodeContext));

        NodeContext nodeContext = getConfiguration()
            // Case where Nomad is bootstrapped from the EnterpriseConfigurationProvider using the old startup script with --node-name and -r.
            // We only know the node name, the the node will start in diagnostic mode
            // So we create an empty cluster / node topology
            .orElseGet(() -> new NodeContext(Node.newDefaultNode(nodeName, parameterSubstitutor.substitute(Setting.NODE_HOSTNAME.getDefaultValue()))));

        this.dynamicConfigService = new DynamicConfigServiceImpl(nodeContext, this);

        registerDiagnosticService();
        LOGGER.info("Successfully initialized NomadServerManager");
      } catch (Exception e) {
        throw new NomadConfigurationException("Exception initializing Nomad Server: " + e.getMessage(), e);
      }
    }

    private void init(Path repositoryPath, IParameterSubstitutor parameterSubstitutor, ConfigChangeHandlerManager manager, NodeContext nodeContext) throws NomadConfigurationException {
      try {
        this.parameterSubstitutor = parameterSubstitutor;
        this.configChangeHandlerManager = manager;
        this.repositoryManager = createNomadRepositoryManager(repositoryPath, parameterSubstitutor);
        this.repositoryManager.createDirectories();
        this.nomadServer = createServer(repositoryManager, nodeContext.getNodeName(), parameterSubstitutor, (version, updatedNodeContext) -> dynamicConfigService.newTopologyCommitted(version, updatedNodeContext));

        this.dynamicConfigService = new DynamicConfigServiceImpl(nodeContext, this);

        registerDiagnosticService();
        LOGGER.info("Successfully initialized NomadServerManager");
      } catch (Exception e) {
        throw new NomadConfigurationException("Exception initializing Nomad Server: " + e.getMessage(), e);
      }
    }

    @SuppressWarnings("unchecked")
    private void registerDiagnosticService() {
      DiagnosticServices.register(IParameterSubstitutor.class, parameterSubstitutor);
      DiagnosticServices.register(ConfigChangeHandlerManager.class, configChangeHandlerManager);
      DiagnosticServices.register(TopologyService.class, dynamicConfigService);
      DiagnosticServices.register(DynamicConfigService.class, dynamicConfigService);
      DiagnosticServices.register(DynamicConfigEventing.class, dynamicConfigService);
      DiagnosticServicesRegistration<NomadServer<String>> registration = (DiagnosticServicesRegistration<NomadServer<String>>) (DiagnosticServicesRegistration) DiagnosticServices.register(NomadServer.class, nomadServer);
      registration.registerMBean(DiagnosticConstants.MBEAN_NOMAD);
    }

    public NomadRepositoryManager getRepositoryManager() {
      return repositoryManager;
    }

    /**
     * Makes Nomad server capable of write operations.
     *
     * @param stripeId        ID of the stripe where the node belongs, should be greater than 1
     * @param nodeName        Name of the running node, non-null
     * @param expectedCluster The cluster coming from the topology entity, when upgrading Nomad to start it.
     *                        This cluster will be also sent next in a ActivationNomadChange, that will make
     *                        sure it receives the expected cluster that has been lastly set in the topology service
     */
    public void upgradeForWrite(int stripeId, String nodeName, Cluster expectedCluster) {
      requireNonNull(nodeName);
      if (stripeId < 1) {
        throw new IllegalArgumentException("Stripe ID should be greater than or equal to 1");
      }

      RoutingNomadChangeProcessor router = new RoutingNomadChangeProcessor()
          .register(SettingNomadChange.class, new SettingNomadChangeProcessor(dynamicConfigService, configChangeHandlerManager, dynamicConfigService::newConfigurationChange))
          .register(ClusterActivationNomadChange.class, new ClusterActivationNomadChangeProcessor(stripeId, nodeName, expectedCluster));

      nomadServer.setChangeApplicator(new ConfigChangeApplicator(new ApplicabilityNomadChangeProcessor(stripeId, nodeName, router)));
      LOGGER.debug("Successfully completed upgradeForWrite procedure");
    }

    /**
     * Used for getting the configuration string from Nomad
     *
     * @return Stored configuration as a String
     * @throws NomadConfigurationException if configuration is unavailable or corrupted
     */
    public Optional<NodeContext> getConfiguration() throws NomadConfigurationException {
      ChangeDetails<NodeContext> latestChange;
      try {
        latestChange = nomadServer.discover().getLatestChange();
      } catch (NomadException e) {
        throw new NomadConfigurationException("Exception while making discover call to Nomad", e);
      }

      if (latestChange == null) {
        return Optional.empty();
      }

      NodeContext configuration = latestChange.getResult();
      if (configuration == null) {
        return Optional.empty();
      }

      return Optional.of(configuration);
    }

    /**
     * Used for overriding corrupted repository content
     *
     * @param configuration Configuration string which will override the existing configuration (if any)
     * @param version       Version of the configuration. This can be used to keep the configuration version in sync with other
     *                      servers.
     * @throws NomadConfigurationException if underlying server fails to override corrupted repository content
     */
    public void repairConfiguration(Cluster configuration, long version) throws NomadConfigurationException {
      try {
        UUID nomadRequestId = UUID.randomUUID();
        DiscoverResponse<NodeContext> discoverResponse = nomadServer.discover();
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

    private UpgradableNomadServer<NodeContext> createServer(NomadRepositoryManager repositoryManager,
                                                            String nodeName,
                                                            IParameterSubstitutor parameterSubstitutor,
                                                            BiConsumer<Long, NodeContext> changeCommitted) throws SanskritException, NomadException {
      return UpgradableNomadServerFactory.createServer(repositoryManager, null, nodeName, parameterSubstitutor, changeCommitted);
    }

    private NomadRepositoryManager createNomadRepositoryManager(Path repositoryPath, IParameterSubstitutor parameterSubstitutor) {
      return new NomadRepositoryManager(repositoryPath, parameterSubstitutor);
    }

    private String getHost() {
      return nomadEnvironment.getHost();
    }

    private String getUser() {
      return nomadEnvironment.getUser();
    }

    public DynamicConfigServiceImpl getDynamicConfigService() {
      return dynamicConfigService;
    }
  }
}
