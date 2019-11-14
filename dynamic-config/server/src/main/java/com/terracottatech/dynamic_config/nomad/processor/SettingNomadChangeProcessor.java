/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.nomad.processor;

import com.terracottatech.dynamic_config.diagnostic.TopologyService;
import com.terracottatech.dynamic_config.handler.ConfigChangeHandler;
import com.terracottatech.dynamic_config.handler.ConfigChangeHandlerManager;
import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.Configuration;
import com.terracottatech.dynamic_config.model.NodeContext;
import com.terracottatech.dynamic_config.nomad.SettingNomadChange;
import com.terracottatech.dynamic_config.util.IParameterSubstitutor;
import com.terracottatech.nomad.server.NomadException;

import java.util.function.BiConsumer;

import static java.util.Objects.requireNonNull;

/**
 * Supports the processing of {@link SettingNomadChange} for dynamic configuration.
 * <p>
 * This Nomad processor finds the {@code ConfigChangeHandlerManager} registered in the diagnostic services
 * to fire the Nomad change request to the right handler
 */
public class SettingNomadChangeProcessor implements NomadChangeProcessor<SettingNomadChange> {

  private final TopologyService topologyService;
  private final ConfigChangeHandlerManager manager;
  private final IParameterSubstitutor parameterSubstitutor;
  private final BiConsumer<Configuration, Boolean> changeListener;

  public SettingNomadChangeProcessor(TopologyService topologyService, ConfigChangeHandlerManager manager, IParameterSubstitutor parameterSubstitutor, BiConsumer<Configuration, Boolean> changeListener) {
    this.topologyService = requireNonNull(topologyService);
    this.manager = requireNonNull(manager);
    this.parameterSubstitutor = requireNonNull(parameterSubstitutor);
    this.changeListener = requireNonNull(changeListener);
  }

  @Override
  public NodeContext tryApply(NodeContext baseConfig, SettingNomadChange change) throws NomadException {
    try {
      // Note the call to baseConfig.clone() which is important
      NodeContext clone = baseConfig.clone();
      Configuration configuration = change.toConfiguration(clone.getCluster());

      configuration.validate(change.getOperation(), parameterSubstitutor);

      Cluster updated = getConfigChangeHandlerManager(change).tryApply(clone, configuration);

      return updated == null ? // null marks the change as rejected
          null :
          baseConfig.getCluster().equals(updated) ? // check if there has been an update
              baseConfig :
              new NodeContext(updated, baseConfig.getStripeId(), baseConfig.getNodeName());
    } catch (Exception e) {
      throw new NomadException(e.getMessage(), e);
    }
  }

  @Override
  public void apply(SettingNomadChange change) {
    NodeContext runtimeNodeContext = topologyService.getRuntimeNodeContext();
    Configuration configuration = change.toConfiguration(runtimeNodeContext.getCluster());
    boolean changeAppliedAtRuntime = getConfigChangeHandlerManager(change).apply(configuration);
    changeListener.accept(configuration, changeAppliedAtRuntime);
  }

  private ConfigChangeHandler getConfigChangeHandlerManager(SettingNomadChange change) {
    return manager.findConfigChangeHandler(change.getSetting())
        .orElseThrow(() -> new IllegalStateException("No " + ConfigChangeHandler.class.getName() + " found for setting " + change.getSetting()));
  }
}
