/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.server.service.handler;

import com.terracottatech.config.data_roots.DataDirectoriesConfig;
import com.terracottatech.dynamic_config.api.model.Cluster;
import com.terracottatech.dynamic_config.api.model.Configuration;
import com.terracottatech.dynamic_config.api.model.NodeContext;
import com.terracottatech.dynamic_config.api.service.ConfigChangeHandler;
import com.terracottatech.dynamic_config.api.service.IParameterSubstitutor;
import com.terracottatech.dynamic_config.api.service.InvalidConfigChangeException;

/**
 * Handles dynamic data-directory additions
 */
public class DataDirectoryConfigChangeHandler implements ConfigChangeHandler {
  private final DataDirectoriesConfig dataDirectoriesConfig;
  private final IParameterSubstitutor parameterSubstitutor;

  public DataDirectoryConfigChangeHandler(DataDirectoriesConfig dataDirectoriesConfig, IParameterSubstitutor parameterSubstitutor) {
    this.dataDirectoriesConfig = dataDirectoriesConfig;
    this.parameterSubstitutor = parameterSubstitutor;
  }

  @Override
  public Cluster tryApply(NodeContext baseConfig, Configuration change) throws InvalidConfigChangeException {
    if (change.getValue() == null) {
      throw new InvalidConfigChangeException("Invalid change: " + change);
    }

    //TODO [DYNAMIC-CONFIG]: TDB-4711 see if we can detect if a data-dir is in use
    try {
      String dataDirectoryName = change.getKey();
      String dataDirectoryPath = change.getValue();
      dataDirectoriesConfig.validateDataDirectory(dataDirectoryName, dataDirectoryPath);

      Cluster updatedCluster = baseConfig.getCluster();
      change.apply(updatedCluster);
      return updatedCluster;
    } catch (RuntimeException e) {
      throw new InvalidConfigChangeException(e.getMessage(), e);
    }
  }

  @Override
  public boolean apply(Configuration change) {
    String dataDirectoryName = change.getKey();
    String dataDirectoryPath = parameterSubstitutor.substitute(change.getValue());
    dataDirectoriesConfig.addDataDirectory(dataDirectoryName, dataDirectoryPath);
    return true;
  }
}
