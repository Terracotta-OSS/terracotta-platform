/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.startup;

import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.config.ConfigFileContainer;
import com.terracottatech.dynamic_config.model.validation.ClusterValidator;
import com.terracottatech.dynamic_config.model.validation.ConfigFileValidator;
import com.terracottatech.dynamic_config.model.validation.NodeParamsValidator;
import com.terracottatech.dynamic_config.parsing.ConsoleParamsParser;
import com.terracottatech.dynamic_config.util.IParameterSubstitutor;
import com.terracottatech.dynamic_config.util.PropertiesFileLoader;
import com.terracottatech.utilities.Tuple2;

import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import static com.terracottatech.dynamic_config.util.ConfigFileParamsUtils.getNodeId;
import static com.terracottatech.dynamic_config.util.ConfigFileParamsUtils.getProperty;
import static com.terracottatech.dynamic_config.util.ConfigFileParamsUtils.getStripeId;

public class ClusterCreator {
  private final IParameterSubstitutor parameterSubstitutor;

  public ClusterCreator(IParameterSubstitutor parameterSubstitutor) {
    this.parameterSubstitutor = parameterSubstitutor;
  }

  /**
   * Creates a {@code Cluster} object from a config properties file
   *
   * @param configFile  the path to the config properties file, non-null
   * @param clusterName the intended name of the cluster, may be null
   * @return a {@code Cluster} object
   */
  Cluster create(Path configFile, String clusterName) {
    // Load the properties and validate the config file
    Properties properties = new PropertiesFileLoader(configFile).loadProperties();
    String fileName = configFile.toFile().getName(); //Path::getFileName can return null, which trips spotBugs
    ConfigFileValidator configFileValidator = new ConfigFileValidator(fileName, properties);
    configFileValidator.validate();
    validateNodeParams(properties);

    // Create Cluster object once validations are successful
    ConfigFileContainer configFileContainer = new ConfigFileContainer(fileName, properties, clusterName, parameterSubstitutor);
    Cluster cluster = configFileContainer.createCluster();
    new ClusterValidator(cluster).validate();
    return cluster;
  }

  /**
   * Creates a {@code Cluster} object from a parameter-value mapping constructed from user input.
   *
   * @param paramValueMap parameter-value mapping
   * @return a {@code Cluster} object
   */
  Cluster create(Map<String, String> paramValueMap) {
    NodeParamsValidator nodeParamsValidator = new NodeParamsValidator(paramValueMap, parameterSubstitutor);
    nodeParamsValidator.validate();
    return new ConsoleParamsParser(paramValueMap, parameterSubstitutor).parse();
  }

  private void validateNodeParams(Properties properties) {
    properties.entrySet()
        .stream()
        .collect(
            Collectors.groupingBy(
                entry -> Tuple2.tuple2(getStripeId(entry.getKey().toString()), getNodeId(entry.getKey().toString())),
                Collectors.toMap(entry -> getProperty(entry.getKey().toString()), entry -> entry.getValue().toString())
            )
        )
        .forEach((nodeIdentifier, map) -> new NodeParamsValidator(map, parameterSubstitutor).validate());
  }
}
