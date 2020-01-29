/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */

package org.terracotta.dynamic_config.server.nomad;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.diagnostic.common.DiagnosticConstants;
import org.terracotta.diagnostic.server.DiagnosticServices;
import org.terracotta.diagnostic.server.DiagnosticServicesRegistration;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.Setting;
import org.terracotta.dynamic_config.api.model.nomad.ClusterActivationNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.SettingNomadChange;
import org.terracotta.dynamic_config.api.service.ConfigChangeHandlerManager;
import org.terracotta.dynamic_config.api.service.DynamicConfigEventService;
import org.terracotta.dynamic_config.api.service.DynamicConfigListener;
import org.terracotta.dynamic_config.api.service.DynamicConfigListenerAdapter;
import org.terracotta.dynamic_config.api.service.DynamicConfigService;
import org.terracotta.dynamic_config.api.service.IParameterSubstitutor;
import org.terracotta.dynamic_config.api.service.LicenseParser;
import org.terracotta.dynamic_config.api.service.LicenseParserDiscovery;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.dynamic_config.server.nomad.processor.ApplicabilityNomadChangeProcessor;
import org.terracotta.dynamic_config.server.nomad.processor.ClusterActivationNomadChangeProcessor;
import org.terracotta.dynamic_config.server.nomad.processor.RoutingNomadChangeProcessor;
import org.terracotta.dynamic_config.server.nomad.processor.SettingNomadChangeProcessor;
import org.terracotta.dynamic_config.server.nomad.repository.NomadRepositoryManager;
import org.terracotta.dynamic_config.server.service.DynamicConfigServiceImpl;
import org.terracotta.nomad.NomadEnvironment;
import org.terracotta.nomad.server.NomadException;
import org.terracotta.nomad.server.NomadServer;
import org.terracotta.nomad.server.UpgradableNomadServer;
import org.terracotta.persistence.sanskrit.SanskritException;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

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
    private volatile DynamicConfigListener listener;

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
      // Case where Nomad is bootstrapped from the EnterpriseConfigurationProvider using the old startup script with --node-name and -r.
      // We only know the node name, the the node will start in diagnostic mode
      // So we create an empty cluster / node topology
      init(repositoryPath, parameterSubstitutor, manager,
          () -> nodeName,
          () -> getConfiguration().orElseGet(
              () -> new NodeContext(Node.newDefaultNode(nodeName, parameterSubstitutor.substitute(Setting.NODE_HOSTNAME.getDefaultValue())))));
    }

    private void init(Path repositoryPath, IParameterSubstitutor parameterSubstitutor, ConfigChangeHandlerManager manager, NodeContext nodeContext) throws NomadConfigurationException {
      init(repositoryPath, parameterSubstitutor, manager, nodeContext::getNodeName, () -> nodeContext);
    }

    private void init(Path repositoryPath, IParameterSubstitutor parameterSubstitutor, ConfigChangeHandlerManager manager, Supplier<String> nodeName, Supplier<NodeContext> nodeContext) throws NomadConfigurationException {
      this.parameterSubstitutor = parameterSubstitutor;
      this.configChangeHandlerManager = manager;
      this.repositoryManager = createNomadRepositoryManager(repositoryPath, parameterSubstitutor);
      this.repositoryManager.createDirectories();
      this.listener = new DynamicConfigListenerAdapter(this::getDynamicConfigService);
      this.nomadServer = createServer(repositoryManager, nodeName.get(), parameterSubstitutor, listener);

      LicenseParser licenseParser = new LicenseParserDiscovery().find().orElseGet(LicenseParser::unsupported);

      this.dynamicConfigService = new DynamicConfigServiceImpl(nodeContext.get(), licenseParser, this);
      registerDiagnosticService();

      LOGGER.info("Successfully initialized NomadServerManager");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void registerDiagnosticService() {
      DiagnosticServices.register(IParameterSubstitutor.class, parameterSubstitutor);
      DiagnosticServices.register(ConfigChangeHandlerManager.class, configChangeHandlerManager);
      DiagnosticServices.register(TopologyService.class, dynamicConfigService);
      DiagnosticServices.register(DynamicConfigService.class, dynamicConfigService);
      DiagnosticServices.register(DynamicConfigEventService.class, dynamicConfigService);
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
          .register(SettingNomadChange.class, new SettingNomadChangeProcessor(dynamicConfigService, configChangeHandlerManager, listener))
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
      try {
        return nomadServer.getCurrentCommittedChangeResult();
      } catch (NomadException e) {
        throw new NomadConfigurationException("Exception while making discover call to Nomad", e);
      }
    }

    private UpgradableNomadServer<NodeContext> createServer(NomadRepositoryManager repositoryManager,
                                                            String nodeName,
                                                            IParameterSubstitutor parameterSubstitutor,
                                                            DynamicConfigListener listener) {
      try {
        return UpgradableNomadServerFactory.createServer(repositoryManager, null, nodeName, parameterSubstitutor, listener);
      } catch (SanskritException | NomadException e) {
        throw new NomadConfigurationException("Exception initializing Nomad Server: " + e.getMessage(), e);
      }
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
