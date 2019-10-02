/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.startup;

import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.Setting;
import com.terracottatech.dynamic_config.model.config.ConfigurationParser;
import com.terracottatech.dynamic_config.model.validation.ClusterValidator;
import com.terracottatech.dynamic_config.parsing.ConsoleParamsUtils;
import com.terracottatech.dynamic_config.util.IParameterSubstitutor;
import com.terracottatech.dynamic_config.util.PropertiesFileLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static java.lang.System.lineSeparator;

public class ClusterCreator {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClusterCreator.class);

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
    Properties properties = new PropertiesFileLoader(configFile).loadProperties();
    Cluster cluster = ConfigurationParser.parsePropertyConfiguration(parameterSubstitutor, properties);
    if (clusterName != null) {
      cluster.setName(clusterName);
    }
    return validated(cluster);
  }

  /**
   * Creates a {@code Cluster} object from a parameter-value mapping constructed from user input.
   *
   * @param paramValueMap parameter-value mapping
   * @return a {@code Cluster} object
   */
  Cluster create(Map<Setting, String> paramValueMap) {
    // safe copy
    paramValueMap = new HashMap<>(paramValueMap);

    Map<Setting, String> defaultsAdded = new TreeMap<>(Comparator.comparing(Setting::toString));
    Cluster cluster = ConfigurationParser.parseCommandLineParameters(parameterSubstitutor, paramValueMap, defaultsAdded::put);

    LOGGER.info(
        String.format(
            "%sRead the following parameters: %s%sAdded the following defaults: %s",
            lineSeparator(),
            toDisplayParams(paramValueMap),
            lineSeparator(),
            toDisplayParams(defaultsAdded)
        )
    );

    return validated(cluster);
  }

  private Cluster validated(Cluster cluster) {
    new ClusterValidator(cluster, parameterSubstitutor).validate();
    return cluster;
  }

  private String toDisplayParams(Map<Setting, String> supplied) {
    String suppliedParameters = supplied.entrySet().stream().sorted(Comparator.comparing(Map.Entry::getKey))
        .map(entry -> ConsoleParamsUtils.addDashDash(entry.getKey()) + "=" + parameterSubstitutor.substitute(entry.getValue()))
        .collect(Collectors.joining(lineSeparator() + "    ", "    ", ""));
    if (suppliedParameters.trim().isEmpty()) {
      suppliedParameters = "[]";
    } else {
      suppliedParameters = lineSeparator() + suppliedParameters;
    }
    return suppliedParameters;
  }
}
