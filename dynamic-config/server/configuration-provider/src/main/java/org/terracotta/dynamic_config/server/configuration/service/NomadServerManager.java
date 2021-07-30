/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.dynamic_config.server.configuration.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.nomad.ClusterActivationNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.FormatUpgradeNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.LockConfigNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.NodeAdditionNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.NodeRemovalNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.SettingNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.StripeAdditionNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.StripeRemovalNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.UnlockConfigNomadChange;
import org.terracotta.dynamic_config.api.service.DynamicConfigService;
import org.terracotta.dynamic_config.api.service.IParameterSubstitutor;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.dynamic_config.server.api.ConfigChangeHandlerManager;
import org.terracotta.dynamic_config.server.api.DelegatingDynamicConfigNomadServer;
import org.terracotta.dynamic_config.server.api.DynamicConfigEventFiring;
import org.terracotta.dynamic_config.server.api.DynamicConfigEventService;
import org.terracotta.dynamic_config.server.api.DynamicConfigNomadServer;
import org.terracotta.dynamic_config.server.api.LicenseService;
import org.terracotta.dynamic_config.server.api.NomadPermissionChangeProcessor;
import org.terracotta.dynamic_config.server.api.NomadRoutingChangeProcessor;
import org.terracotta.dynamic_config.server.configuration.nomad.NomadServerFactory;
import org.terracotta.dynamic_config.server.configuration.nomad.persistence.ConfigStorageException;
import org.terracotta.dynamic_config.server.configuration.nomad.persistence.NomadConfigurationManager;
import org.terracotta.dynamic_config.server.configuration.service.nomad.processor.ApplicabilityNomadChangeProcessor;
import org.terracotta.dynamic_config.server.configuration.service.nomad.processor.ClusterActivationNomadChangeProcessor;
import org.terracotta.dynamic_config.server.configuration.service.nomad.processor.DefaultNomadRoutingChangeProcessor;
import org.terracotta.dynamic_config.server.configuration.service.nomad.processor.FormatUpgradeNomadChangeProcessor;
import org.terracotta.dynamic_config.server.configuration.service.nomad.processor.LockAwareNomadChangeProcessor;
import org.terracotta.dynamic_config.server.configuration.service.nomad.processor.LockConfigNomadChangeProcessor;
import org.terracotta.dynamic_config.server.configuration.service.nomad.processor.MultiSettingNomadChangeProcessor;
import org.terracotta.dynamic_config.server.configuration.service.nomad.processor.NodeAdditionNomadChangeProcessor;
import org.terracotta.dynamic_config.server.configuration.service.nomad.processor.NodeRemovalNomadChangeProcessor;
import org.terracotta.dynamic_config.server.configuration.service.nomad.processor.NomadPermissionChangeProcessorImpl;
import org.terracotta.dynamic_config.server.configuration.service.nomad.processor.SettingNomadChangeProcessor;
import org.terracotta.dynamic_config.server.configuration.service.nomad.processor.StripeAdditionNomadChangeProcessor;
import org.terracotta.dynamic_config.server.configuration.service.nomad.processor.StripeRemovalNomadChangeProcessor;
import org.terracotta.dynamic_config.server.configuration.service.nomad.processor.UnlockConfigNomadChangeProcessor;
import org.terracotta.json.ObjectMapperFactory;
import org.terracotta.nomad.server.NomadException;
import org.terracotta.nomad.server.NomadServerMode;
import org.terracotta.nomad.server.UncheckedNomadException;
import org.terracotta.persistence.sanskrit.SanskritException;
import org.terracotta.server.Server;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * @author Mathieu Carbou
 */
public class NomadServerManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(NomadServerManager.class);

  private final ObjectMapperFactory objectMapperFactory;
  private final NomadServerFactory nomadServerFactory;
  private final IParameterSubstitutor parameterSubstitutor;
  private final ConfigChangeHandlerManager configChangeHandlerManager;
  private final LicenseService licenseService;
  private final Server server;
  private final DefaultNomadRoutingChangeProcessor router = new DefaultNomadRoutingChangeProcessor();
  private final NomadPermissionChangeProcessorImpl nomadPermissionChangeProcessor = new NomadPermissionChangeProcessorImpl();
  private final DynamicConfigEventService eventRegistrationService;
  private final DynamicConfigEventFiring eventFiringService;
  private final DelegatingDynamicConfigNomadServer nomadServer = new DelegatingDynamicConfigNomadServer(DynamicConfigNomadServer.empty());

  private volatile NomadConfigurationManager configurationManager;
  private volatile DynamicConfigService dynamicConfigService;
  private volatile TopologyService topologyService;

  public NomadServerManager(IParameterSubstitutor parameterSubstitutor,
                            ConfigChangeHandlerManager configChangeHandlerManager,
                            LicenseService licenseService,
                            ObjectMapperFactory objectMapperFactory,
                            Server server) {
    this.objectMapperFactory = requireNonNull(objectMapperFactory);
    this.nomadServerFactory = new NomadServerFactory(objectMapperFactory);
    this.parameterSubstitutor = requireNonNull(parameterSubstitutor);
    this.configChangeHandlerManager = requireNonNull(configChangeHandlerManager);
    this.licenseService = requireNonNull(licenseService);
    this.server = server;

    // the eventFiringService is used by callers to fire events
    // events are received by the DC service, processed, and fired back to the listeners registered in the event service
    DynamicConfigEventServiceImpl eventService = new DynamicConfigEventServiceImpl();
    this.eventRegistrationService = eventService;
    this.eventFiringService = eventService;
  }

  public DynamicConfigService getDynamicConfigService() {
    if (dynamicConfigService == null) {
      throw new AssertionError("Not initialized");
    }
    return dynamicConfigService;
  }

  public TopologyService getTopologyService() {
    if (topologyService == null) {
      throw new AssertionError("Not initialized");
    }
    return topologyService;
  }

  public NomadConfigurationManager getConfigurationManager() {
    if (configurationManager == null) {
      throw new AssertionError("Not initialized");
    }
    return configurationManager;
  }

  public DynamicConfigNomadServer getNomadServer() {
    return nomadServer;
  }

  public DynamicConfigEventFiring getEventFiringService() {
    return eventFiringService;
  }

  public DynamicConfigEventService getEventRegistrationService() {
    return eventRegistrationService;
  }

  public NomadRoutingChangeProcessor getNomadRoutingChangeProcessor() {
    return router;
  }

  public NomadPermissionChangeProcessor getNomadPermissionChangeProcessor() {
    return nomadPermissionChangeProcessor;
  }

  /**
   * Reloads from an existing config repository.
   * This will initialize the nomad system based on a specific config directory.
   */
  public synchronized void reload(Path configPath, String nodeName, NodeContext alternate) throws UncheckedNomadException {
    assertNotInitialized();

    if (this.nomadServer.getNomadServerMode() != NomadServerMode.UNINITIALIZED) {
      throw new AssertionError();
    }

    LOGGER.debug("reload({}, {}, {})", configPath, nodeName, alternate);

    // Case where Nomad is bootstrapped from an existing configuration directory.
    // We only know the node name.
    // getConfiguration() can be empty in case the repo has been created
    // but not yet populated with some Nomad entries, or it was reset, or tx was prepared but not committed.
    // In these cases, node will start in diagnostic mode and use an alternate topology.
    requireNonNull(configPath);
    requireNonNull(nodeName);
    requireNonNull(alternate);

    if (!Files.isDirectory(configPath)) {
      // Note: this validation is done also upfront in the CLI. This one is for dev purposes.
      throw new IllegalArgumentException("Path not found (or not  directory): " + configPath);
    }

    NodeContext startingConfig;
    try {
      this.configurationManager = new NomadConfigurationManager(configPath, parameterSubstitutor);
      this.nomadServer.setDelegate(nomadServerFactory.createServer(configurationManager, nodeName, eventFiringService));
      startingConfig = nomadServer.getCurrentCommittedConfig().orElse(alternate);
    } catch (SanskritException | NomadException | ConfigStorageException e) {
      throw new UncheckedNomadException("Exception initializing Nomad Server: " + e.getMessage(), e);
    }

    DynamicConfigServiceImpl dynamicConfigService = new DynamicConfigServiceImpl(startingConfig, licenseService, this, objectMapperFactory, server);
    this.dynamicConfigService = new AuditService(dynamicConfigService, server);
    this.topologyService = dynamicConfigService;

    eventRegistrationService.register(dynamicConfigService);
    eventRegistrationService.register(new AuditListener(server));

    LOGGER.info("Bootstrapped nomad system with root: {}", parameterSubstitutor.substitute(configPath.toString()));
  }

  /**
   * Configures a node in-memory without the Nomad system.
   * No config directories are created and configPath should point to an inexisting folder.
   */
  public synchronized void configure(Path configPath, NodeContext nodeContext) throws UncheckedNomadException {
    assertNotInitialized();

    if (this.nomadServer.getNomadServerMode() != NomadServerMode.UNINITIALIZED) {
      throw new AssertionError();
    }

    LOGGER.debug("configure({}, {})", configPath, nodeContext);

    requireNonNull(configPath);
    requireNonNull(nodeContext);

    this.configurationManager = new NomadConfigurationManager(configPath, parameterSubstitutor);

    DynamicConfigServiceImpl dynamicConfigService = new DynamicConfigServiceImpl(nodeContext, licenseService, this, objectMapperFactory, server);
    this.dynamicConfigService = new AuditService(dynamicConfigService, server);
    this.topologyService = dynamicConfigService;

    eventRegistrationService.register(dynamicConfigService);
    eventRegistrationService.register(new AuditListener(server));

    LOGGER.info("Bootstrapped nomad system with root: {}", parameterSubstitutor.substitute(configPath.toString()));
  }

  /**
   * This method can be called to create the initial config repository when the node will be activated.
   * It can only be called after the {@link #configure(Path, NodeContext)} method.
   */
  public synchronized void initNomad() {
    assertInitialized();

    if (this.nomadServer.getNomadServerMode() == NomadServerMode.UNINITIALIZED) {
      NodeContext nodeContext = topologyService.getUpcomingNodeContext();
      LOGGER.debug("initNomad({})", nodeContext);

      this.configurationManager.createDirectories();
      try {
        this.nomadServer.setDelegate(nomadServerFactory.createServer(configurationManager, nodeContext.getNode().getName(), getEventFiringService()));
      } catch (SanskritException | NomadException | ConfigStorageException e) {
        throw new UncheckedNomadException("Exception initializing Nomad Server: " + e.getMessage(), e);
      }
    }
  }

  /**
   * This method changes the Nomad ability to support transactions.
   * RO will only allow discovery calls, RW will allow updates.
   * <p>
   * It requires the Nomad system to be initialized first with {@link #reload(Path, String, NodeContext)} or {@link #initNomad()}
   */
  public synchronized void setNomad(NomadMode nomadMode) {
    assertInitialized();

    switch (nomadMode) {

      case RO:
        if (nomadServer.getChangeApplicator() != null) {
          LOGGER.debug("setNomad({})", nomadMode);
          nomadServer.setChangeApplicator(null);
        }
        break;

      case RW:
        if (nomadServer.getChangeApplicator() == null) {
          if (this.nomadServer.getNomadServerMode() == NomadServerMode.UNINITIALIZED) {
            throw new AssertionError();
          }

          LOGGER.debug("setNomad({})", nomadMode);

          NodeContext nodeContext = topologyService.getUpcomingNodeContext();

          router.clear();
          router.register(SettingNomadChange.class, new SettingNomadChangeProcessor(getTopologyService(), configChangeHandlerManager, getEventFiringService()));
          router.register(NodeRemovalNomadChange.class, new NodeRemovalNomadChangeProcessor(server.getManagement().getMBeanServer(), getTopologyService(), getEventFiringService()));
          router.register(NodeAdditionNomadChange.class, new NodeAdditionNomadChangeProcessor(server.getManagement().getMBeanServer(), getTopologyService(), getEventFiringService()));
          router.register(ClusterActivationNomadChange.class, new ClusterActivationNomadChangeProcessor(nodeContext.getNodeUID()));
          router.register(StripeAdditionNomadChange.class, new StripeAdditionNomadChangeProcessor(getTopologyService(), getEventFiringService(), licenseService));
          router.register(StripeRemovalNomadChange.class, new StripeRemovalNomadChangeProcessor(getTopologyService(), getEventFiringService()));
          router.register(FormatUpgradeNomadChange.class, new FormatUpgradeNomadChangeProcessor());
          router.register(LockConfigNomadChange.class, new LockConfigNomadChangeProcessor());
          router.register(UnlockConfigNomadChange.class, new UnlockConfigNomadChangeProcessor());

          nomadServer.setChangeApplicator(
              new ConfigChangeApplicator(
                  nodeContext.getNodeUID(),
                  new LockAwareNomadChangeProcessor(
                      new MultiSettingNomadChangeProcessor(
                          nomadPermissionChangeProcessor.then(new ApplicabilityNomadChangeProcessor(getTopologyService(), router))
                      )
                  )
              )
          );
        }
        break;

      default:
        throw new AssertionError(nomadMode);
    }
  }

  public synchronized void reset() throws NomadException {
    nomadServer.reset();
    setNomad(NomadMode.RO);
  }

  public NomadMode getNomadMode() {
    return nomadServer.getChangeApplicator() == null ? NomadMode.RO : NomadMode.RW;
  }

  /**
   * Used for getting the configuration string from Nomad
   *
   * @return Stored configuration as a String
   * @throws UncheckedNomadException if configuration is unavailable or corrupted
   */
  public Optional<NodeContext> getConfiguration() throws UncheckedNomadException {
    try {
      return getNomadServer().getCurrentCommittedConfig();
    } catch (NomadException e) {
      throw new UncheckedNomadException("Exception while making discover call to Nomad", e);
    }
  }

  private void assertNotInitialized() {
    if (this.configurationManager != null || this.dynamicConfigService != null || this.topologyService != null) {
      throw new AssertionError();
    }
  }

  private void assertInitialized() {
    if (this.configurationManager == null || this.dynamicConfigService == null || this.topologyService == null) {
      throw new AssertionError();
    }
  }
}
