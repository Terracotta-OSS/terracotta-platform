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
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.Setting;
import org.terracotta.dynamic_config.api.model.nomad.ClusterActivationNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.NodeAdditionNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.NodeRemovalNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.SettingNomadChange;
import org.terracotta.dynamic_config.api.service.IParameterSubstitutor;
import org.terracotta.dynamic_config.server.api.ConfigChangeHandlerManager;
import org.terracotta.dynamic_config.server.api.DynamicConfigListener;
import org.terracotta.dynamic_config.server.api.DynamicConfigListenerAdapter;
import org.terracotta.dynamic_config.server.api.LicenseService;
import org.terracotta.dynamic_config.server.api.RoutingNomadChangeProcessor;
import org.terracotta.dynamic_config.server.configuration.nomad.ConfigChangeApplicator;
import org.terracotta.dynamic_config.server.configuration.nomad.NomadServerFactory;
import org.terracotta.dynamic_config.server.configuration.nomad.UncheckedNomadException;
import org.terracotta.dynamic_config.server.configuration.nomad.persistence.NomadRepositoryManager;
import org.terracotta.dynamic_config.server.configuration.nomad.processor.ApplicabilityNomadChangeProcessor;
import org.terracotta.dynamic_config.server.configuration.nomad.processor.ClusterActivationNomadChangeProcessor;
import org.terracotta.dynamic_config.server.configuration.nomad.processor.DefaultRoutingNomadChangeProcessor;
import org.terracotta.dynamic_config.server.configuration.nomad.processor.MultiSettingNomadChangeProcessor;
import org.terracotta.dynamic_config.server.configuration.nomad.processor.NodeAdditionNomadChangeProcessor;
import org.terracotta.dynamic_config.server.configuration.nomad.processor.NodeRemovalNomadChangeProcessor;
import org.terracotta.dynamic_config.server.configuration.nomad.processor.SettingNomadChangeProcessor;
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

  private final IParameterSubstitutor parameterSubstitutor;
  private final ConfigChangeHandlerManager configChangeHandlerManager;
  private final LicenseService licenseService;
  private final DynamicConfigListener dynamicConfigListener;

  private volatile UpgradableNomadServer<NodeContext> nomadServer;
  private volatile NomadRepositoryManager repositoryManager;
  private volatile DynamicConfigServiceImpl dynamicConfigService;
  private volatile RoutingNomadChangeProcessor routingNomadChangeProcessor;

  public NomadServerManager(IParameterSubstitutor parameterSubstitutor, ConfigChangeHandlerManager configChangeHandlerManager, LicenseService licenseService) {
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

  public NomadRepositoryManager getRepositoryManager() {
    return repositoryManager;
  }

  public Optional<RoutingNomadChangeProcessor> getRoutingNomadChangeProcessor() {
    return Optional.ofNullable(routingNomadChangeProcessor);
  }

  public DynamicConfigListener getDynamicConfigListener() {
    return dynamicConfigListener;
  }

  public void init(Path repositoryPath, String nodeName) throws UncheckedNomadException {
    // Case where Nomad is bootstrapped from an existing config repository
    // We only know the node name, the the node will start in diagnostic mode
    // So we create an empty cluster / node topology
    init(repositoryPath,
        () -> nodeName,
        () -> getConfiguration().orElseGet(
            () -> new NodeContext(Node.newDefaultNode(nodeName, parameterSubstitutor.substitute(Setting.NODE_HOSTNAME.getDefaultValue())))));
  }

  public void init(Path repositoryPath, NodeContext nodeContext) throws UncheckedNomadException {
    init(repositoryPath, nodeContext::getNodeName, () -> nodeContext);
  }

  public void init(Path repositoryPath, Supplier<String> nodeName, Supplier<NodeContext> nodeContext) throws UncheckedNomadException {
    requireNonNull(repositoryPath);
    requireNonNull(nodeName);
    requireNonNull(nodeContext);

    this.repositoryManager = new NomadRepositoryManager(repositoryPath, parameterSubstitutor);
    this.repositoryManager.createDirectories();

    try {
      this.nomadServer = NomadServerFactory.createServer(repositoryManager, null, nodeName.get(), dynamicConfigListener);
    } catch (SanskritException | NomadException e) {
      throw new UncheckedNomadException("Exception initializing Nomad Server: " + e.getMessage(), e);
    }

    this.dynamicConfigService = new DynamicConfigServiceImpl(nodeContext.get(), licenseService, this);

    LOGGER.info("Bootstrapped nomad system with root: {}", parameterSubstitutor.substitute(repositoryPath.toString()));
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

    DefaultRoutingNomadChangeProcessor router = new DefaultRoutingNomadChangeProcessor();
    router.register(SettingNomadChange.class, new SettingNomadChangeProcessor(dynamicConfigService, configChangeHandlerManager, dynamicConfigListener));
    router.register(NodeRemovalNomadChange.class, new NodeRemovalNomadChangeProcessor(dynamicConfigService, dynamicConfigListener));
    router.register(NodeAdditionNomadChange.class, new NodeAdditionNomadChangeProcessor(dynamicConfigService, dynamicConfigListener));
    router.register(ClusterActivationNomadChange.class, new ClusterActivationNomadChangeProcessor(stripeId, nodeName));

    this.routingNomadChangeProcessor = router;

    nomadServer.setChangeApplicator(
        new ConfigChangeApplicator(stripeId, nodeName,  // receives the change and return the allow() or reject() result depending on processors, with the config to write on disk
            new MultiSettingNomadChangeProcessor( // unwrap a MultiNomadChange and call underlying processors for each one
                new ApplicabilityNomadChangeProcessor(stripeId, nodeName, router)))); // filter the nomad change validation and applicability
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
