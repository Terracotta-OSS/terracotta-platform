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
import org.terracotta.dynamic_config.api.model.nomad.NodeAdditionNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.NodeRemovalNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.SettingNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.StripeAdditionNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.StripeRemovalNomadChange;
import org.terracotta.dynamic_config.api.service.IParameterSubstitutor;
import org.terracotta.dynamic_config.server.api.ConfigChangeHandlerManager;
import org.terracotta.dynamic_config.server.api.DynamicConfigListener;
import org.terracotta.dynamic_config.server.api.DynamicConfigListenerAdapter;
import org.terracotta.dynamic_config.server.api.LicenseService;
import org.terracotta.dynamic_config.server.api.NomadPermissionChangeProcessor;
import org.terracotta.dynamic_config.server.api.NomadRoutingChangeProcessor;
import org.terracotta.dynamic_config.server.configuration.nomad.ConfigChangeApplicator;
import org.terracotta.dynamic_config.server.configuration.nomad.NomadServerFactory;
import org.terracotta.dynamic_config.server.configuration.nomad.UncheckedNomadException;
import org.terracotta.dynamic_config.server.configuration.nomad.persistence.NomadConfigurationManager;
import org.terracotta.dynamic_config.server.configuration.service.nomad.processor.ApplicabilityNomadChangeProcessor;
import org.terracotta.dynamic_config.server.configuration.service.nomad.processor.ClusterActivationNomadChangeProcessor;
import org.terracotta.dynamic_config.server.configuration.service.nomad.processor.DefaultNomadRoutingChangeProcessor;
import org.terracotta.dynamic_config.server.configuration.service.nomad.processor.MultiSettingNomadChangeProcessor;
import org.terracotta.dynamic_config.server.configuration.service.nomad.processor.NodeAdditionNomadChangeProcessor;
import org.terracotta.dynamic_config.server.configuration.service.nomad.processor.NodeRemovalNomadChangeProcessor;
import org.terracotta.dynamic_config.server.configuration.service.nomad.processor.NomadPermissionChangeProcessorImpl;
import org.terracotta.dynamic_config.server.configuration.service.nomad.processor.SettingNomadChangeProcessor;
import org.terracotta.dynamic_config.server.configuration.service.nomad.processor.StripeAdditionNomadChangeProcessor;
import org.terracotta.dynamic_config.server.configuration.service.nomad.processor.StripeRemovalNomadChangeProcessor;
import org.terracotta.json.ObjectMapperFactory;
import org.terracotta.nomad.server.NomadException;
import org.terracotta.nomad.server.UpgradableNomadServer;
import org.terracotta.persistence.sanskrit.SanskritException;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Supplier;

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
  private final DynamicConfigListener dynamicConfigListener;
  private final DefaultNomadRoutingChangeProcessor router = new DefaultNomadRoutingChangeProcessor();
  private final NomadPermissionChangeProcessorImpl nomadPermissionChangeProcessor = new NomadPermissionChangeProcessorImpl();

  private volatile UpgradableNomadServer<NodeContext> nomadServer;
  private volatile NomadConfigurationManager configurationManager;
  private volatile DynamicConfigServiceImpl dynamicConfigService;

  public NomadServerManager(IParameterSubstitutor parameterSubstitutor, ConfigChangeHandlerManager configChangeHandlerManager, LicenseService licenseService, ObjectMapperFactory objectMapperFactory) {
    this.objectMapperFactory = requireNonNull(objectMapperFactory);
    this.nomadServerFactory = new NomadServerFactory(objectMapperFactory);
    this.parameterSubstitutor = requireNonNull(parameterSubstitutor);
    this.configChangeHandlerManager = requireNonNull(configChangeHandlerManager);
    this.licenseService = requireNonNull(licenseService);
    this.dynamicConfigListener = new DynamicConfigListenerAdapter(this::getDynamicConfigService);
  }

  public UpgradableNomadServer<NodeContext> getNomadServer() {
    return nomadServer;
  }

  public DynamicConfigServiceImpl getDynamicConfigService() {
    return dynamicConfigService;
  }

  public NomadConfigurationManager getConfigurationManager() {
    return configurationManager;
  }

  public NomadRoutingChangeProcessor getNomadRoutingChangeProcessor() {
    return router;
  }

  public NomadPermissionChangeProcessor getNomadPermissionChangeProcessor() {
    return nomadPermissionChangeProcessor;
  }

  public DynamicConfigListener getDynamicConfigListener() {
    return dynamicConfigListener;
  }

  public void init(Path configPath, String nodeName, NodeContext alternate) throws UncheckedNomadException {
    // Case where Nomad is bootstrapped from an existing configuration directory.
    // We only know the node name.
    // getConfiguration() can be empty in case the repo has been created
    // but not yet populated with some Nomad entries, or it was reset, or tx was prepared but not committed.
    // In these cases, node will start in diagnostic mode and use an alternate topology.
    init(configPath,
        () -> nodeName,
        () -> getConfiguration().orElse(alternate));
  }

  public void init(Path configPath, NodeContext nodeContext) throws UncheckedNomadException {
    init(configPath, nodeContext::getNodeName, () -> nodeContext);
  }

  public void init(Path configPath, Supplier<String> nodeName, Supplier<NodeContext> nodeContext) throws UncheckedNomadException {
    requireNonNull(configPath);
    requireNonNull(nodeName);
    requireNonNull(nodeContext);

    this.configurationManager = new NomadConfigurationManager(configPath, parameterSubstitutor);
    this.configurationManager.createDirectories();

    try {
      this.nomadServer = nomadServerFactory.createServer(configurationManager, null, nodeName.get(), dynamicConfigListener);
    } catch (SanskritException | NomadException e) {
      throw new UncheckedNomadException("Exception initializing Nomad Server: " + e.getMessage(), e);
    }

    this.dynamicConfigService = new DynamicConfigServiceImpl(nodeContext.get(), licenseService, this, objectMapperFactory);

    LOGGER.info("Bootstrapped nomad system with root: {}", parameterSubstitutor.substitute(configPath.toString()));
  }

  public void downgradeForRead() {
    nomadServer.setChangeApplicator(null);
  }

  /**
   * Makes Nomad server capable of write operations.
   *
   * @param stripeId ID of the stripe where the node belongs, should be greater than 1
   * @param nodeName Name of the running node, non-null
   */
  public void upgradeForWrite(int stripeId, String nodeName) {
    requireNonNull(nodeName);
    if (stripeId < 1) {
      throw new IllegalArgumentException("Stripe ID should be greater than or equal to 1");
    }
    if (nomadServer.getChangeApplicator() != null) {
      throw new IllegalStateException("Nomad is already upgraded");
    }

    router.register(SettingNomadChange.class, new SettingNomadChangeProcessor(dynamicConfigService, configChangeHandlerManager, dynamicConfigListener));
    router.register(NodeRemovalNomadChange.class, new NodeRemovalNomadChangeProcessor(dynamicConfigService, dynamicConfigListener));
    router.register(NodeAdditionNomadChange.class, new NodeAdditionNomadChangeProcessor(dynamicConfigService, dynamicConfigListener));
    router.register(ClusterActivationNomadChange.class, new ClusterActivationNomadChangeProcessor(stripeId, nodeName));
    router.register(StripeAdditionNomadChange.class, new StripeAdditionNomadChangeProcessor(dynamicConfigService, dynamicConfigListener, licenseService));
    router.register(StripeRemovalNomadChange.class, new StripeRemovalNomadChangeProcessor(dynamicConfigService, dynamicConfigListener));

    nomadServer.setChangeApplicator(
        new ConfigChangeApplicator(stripeId, nodeName,
            new MultiSettingNomadChangeProcessor(nomadPermissionChangeProcessor
                .then(new ApplicabilityNomadChangeProcessor(stripeId, nodeName, router)))));

    LOGGER.debug("Successfully completed upgradeForWrite procedure");
  }

  /**
   * Used for getting the configuration string from Nomad
   *
   * @return Stored configuration as a String
   * @throws UncheckedNomadException if configuration is unavailable or corrupted
   */
  public Optional<NodeContext> getConfiguration() throws UncheckedNomadException {
    try {
      return nomadServer.getCurrentCommittedChangeResult();
    } catch (NomadException e) {
      throw new UncheckedNomadException("Exception while making discover call to Nomad", e);
    }
  }
}
