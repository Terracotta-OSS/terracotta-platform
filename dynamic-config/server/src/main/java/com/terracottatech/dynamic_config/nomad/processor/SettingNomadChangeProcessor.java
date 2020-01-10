/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.nomad.processor;

import com.terracottatech.dynamic_config.handler.ConfigChangeHandler;
import com.terracottatech.dynamic_config.handler.ConfigChangeHandlerManager;
import com.terracottatech.dynamic_config.handler.InvalidConfigChangeException;
import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.Configuration;
import com.terracottatech.dynamic_config.model.NodeContext;
import com.terracottatech.dynamic_config.nomad.SettingNomadChange;
import com.terracottatech.dynamic_config.service.api.DynamicConfigListener;
import com.terracottatech.dynamic_config.service.api.TopologyService;
import com.terracottatech.nomad.server.NomadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;

/**
 * Supports the processing of {@link SettingNomadChange} for dynamic configuration.
 * <p>
 * This Nomad processor finds the {@code ConfigChangeHandlerManager} registered in the diagnostic services
 * to fire the Nomad change request to the right handler
 */
public class SettingNomadChangeProcessor implements NomadChangeProcessor<SettingNomadChange> {
  private static final Logger LOGGER = LoggerFactory.getLogger(SettingNomadChangeProcessor.class);

  private final TopologyService topologyService;
  private final ConfigChangeHandlerManager manager;
  private final DynamicConfigListener listener;

  public SettingNomadChangeProcessor(TopologyService topologyService, ConfigChangeHandlerManager manager, DynamicConfigListener listener) {
    this.topologyService = requireNonNull(topologyService);
    this.manager = requireNonNull(manager);
    this.listener = requireNonNull(listener);
  }

  @Override
  public NodeContext tryApply(NodeContext baseConfig, SettingNomadChange change) throws NomadException {
    try {
      // Note the call to baseConfig.clone() which is important
      NodeContext clone = baseConfig.clone();
      Configuration configuration = change.toConfiguration(clone.getCluster());
      configuration.validate(change.getOperation());

      ConfigChangeHandler configChangeHandler = getConfigChangeHandlerManager(change);
      LOGGER.debug("NodeContext before tryApply(): {}", clone);
      Cluster updated = configChangeHandler.tryApply(clone, configuration);

      if (updated == null) {
        LOGGER.debug("Change: {} rejected in config change handler: {}", change, configChangeHandler.getClass().getSimpleName());
        return null;
      } else {
        if (baseConfig.getCluster().equals(updated)) {
          LOGGER.debug("Cluster not updated for change: {} in config change handler: {}", change, configChangeHandler.getClass().getSimpleName());
          return baseConfig;
        } else {
          LOGGER.info("Cluster updated to: {} for change: {} in: {}", updated, change, configChangeHandler.getClass().getSimpleName());
          // Make a new NodeContext object just in case a clone of the original Cluster was returned from tryApply
          return new NodeContext(updated, baseConfig.getStripeId(), baseConfig.getNodeName());
        }
      }
    } catch (InvalidConfigChangeException | RuntimeException e) {
      throw new NomadException("Error when trying to apply setting change '" + change.getSummary() + "': " + e.getMessage(), e);
    }
  }

  @Override
  public void apply(SettingNomadChange change) throws NomadException {
    try {
      // try to apply the change on the runtime configuration
      NodeContext runtimeNodeContext = topologyService.getRuntimeNodeContext();
      Configuration configuration = change.toConfiguration(runtimeNodeContext.getCluster());
      boolean changeAppliedAtRuntime = getConfigChangeHandlerManager(change).apply(configuration);
      LOGGER.debug("Change: {} applied at runtime ? {}", change.getSummary(), changeAppliedAtRuntime);

      if (changeAppliedAtRuntime) {
        // configuration was saved following tryApply call, and applied at runtime
        configuration.apply(runtimeNodeContext.getCluster());
        listener.onNewConfigurationAppliedAtRuntime(runtimeNodeContext, configuration);

      } else {
        // configuration was saved following tryApply call, but not applied at runtime because requires a restart
        NodeContext upcomingNodeContext = topologyService.getUpcomingNodeContext();
        Configuration cfg = change.toConfiguration(upcomingNodeContext.getCluster());
        listener.onNewConfigurationPendingRestart(runtimeNodeContext, cfg);
      }
    } catch (RuntimeException e) {
      throw new NomadException("Error when applying setting change '" + change.getSummary() + "': " + e.getMessage(), e);
    }
  }

  private ConfigChangeHandler getConfigChangeHandlerManager(SettingNomadChange change) {
    return manager.findConfigChangeHandler(change.getSetting())
        .orElseThrow(() -> new IllegalStateException("No " + ConfigChangeHandler.class.getName() + " found for setting " + change.getSetting()));
  }
}
