/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.startup;

import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.Setting;
import com.terracottatech.dynamic_config.model.config.ConfigFileParser;
import com.terracottatech.dynamic_config.model.validation.ClusterValidator;
import com.terracottatech.dynamic_config.model.validation.ConfigFileFormatValidator;
import com.terracottatech.dynamic_config.model.validation.NodeParamsValidator;
import com.terracottatech.dynamic_config.parsing.ConsoleParamsParser;
import com.terracottatech.dynamic_config.util.IParameterSubstitutor;
import com.terracottatech.dynamic_config.util.PropertiesFileLoader;

import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

import static com.terracottatech.dynamic_config.util.ConfigFileParamsUtils.getNodeId;
import static com.terracottatech.dynamic_config.util.ConfigFileParamsUtils.getSetting;
import static com.terracottatech.dynamic_config.util.ConfigFileParamsUtils.getStripeId;
import static com.terracottatech.utilities.Tuple2.tuple2;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

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

    ConfigFileFormatValidator configFileValidator = new ConfigFileFormatValidator(configFile, properties);
    configFileValidator.validate();

    properties.entrySet()
        .stream()
        .collect(
            groupingBy(
                entry -> tuple2(getStripeId(entry.getKey().toString()), getNodeId(entry.getKey().toString())),
                toMap(entry -> getSetting(entry.getKey().toString()), entry -> entry.getValue().toString().trim())
            )
        )
        .values()
        .forEach(this::validateNodeSettings);

    // Create Cluster object once validations are successful
    ConfigFileParser configFileParser = new ConfigFileParser(configFile, properties, parameterSubstitutor);
    Cluster cluster = configFileParser.createCluster();
    if (clusterName != null) {
      cluster.setName(clusterName);
    }

    new ClusterValidator(cluster).validate();

    return cluster;
  }

  /**
   * Creates a {@code Cluster} object from a parameter-value mapping constructed from user input.
   *
   * @param paramValueMap parameter-value mapping
   * @return a {@code Cluster} object
   */
  Cluster create(Map<Setting, String> paramValueMap) {
    validateNodeSettings(paramValueMap);
    return new ConsoleParamsParser(paramValueMap, parameterSubstitutor).parse();
  }

  private void validateNodeSettings(Map<Setting, String> map) {
    new NodeParamsValidator(map, parameterSubstitutor).validate();
  }
}
