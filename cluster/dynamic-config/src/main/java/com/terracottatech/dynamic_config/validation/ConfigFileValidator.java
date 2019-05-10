/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.validation;

import com.terracottatech.dynamic_config.config.CommonOptions;
import com.terracottatech.dynamic_config.config.DefaultSettings;
import com.terracottatech.dynamic_config.config.NodeIdentifier;
import com.terracottatech.dynamic_config.exception.MalformedConfigFileException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import static com.terracottatech.dynamic_config.config.CommonOptions.DATA_DIRS;
import static com.terracottatech.dynamic_config.config.CommonOptions.NODE_NAME;
import static com.terracottatech.dynamic_config.config.CommonOptions.OFFHEAP_RESOURCES;
import static com.terracottatech.dynamic_config.util.ConfigFileParamsUtils.getClusterName;
import static com.terracottatech.dynamic_config.util.ConfigFileParamsUtils.getNodeName;
import static com.terracottatech.dynamic_config.util.ConfigFileParamsUtils.getProperty;
import static com.terracottatech.dynamic_config.util.ConfigFileParamsUtils.getStripeName;
import static com.terracottatech.dynamic_config.util.ConfigFileParamsUtils.splitKey;

public class ConfigFileValidator {
  private static final Set<String> ALL_VALID_OPTIONS = CommonOptions.getAllOptions();

  public static Properties validate(File file) {
    Properties properties = loadProperties(file);
    validateProperties(properties, file.getName());
    return properties;
  }

  static void validateProperties(Properties properties, String fileName) {
    properties.forEach((key, value) -> {
      ensureCorrectFieldCount(key.toString(), value.toString(), fileName);
      ensureNoInvalidOptions(key.toString(), fileName);
      ensureCorrectNodeName(key.toString(), value.toString(), fileName);
    });
    ensureOnlyOneClusterName(properties, fileName);
    ensureAllOptionsPresent(properties, fileName);
    ensureMandatoryPropertiesHaveValues(properties, fileName);

    Map<NodeIdentifier, Map<String, String>> nodeParamValueMap = properties.entrySet().stream()
        .collect(
            Collectors.groupingBy(
                entry -> new NodeIdentifier(getStripeName(entry.getKey().toString()), getNodeName(entry.getKey().toString())),
                Collectors.toMap(entry -> getProperty(entry.getKey().toString()), entry -> entry.getValue().toString())
            )
        );
    nodeParamValueMap.forEach((nodeIdentifier, map) -> NodeParamsValidator.validate(map));
  }

  private static void ensureMandatoryPropertiesHaveValues(Properties properties, String fileName) {
    // DefaultSettings contains properties which are mandatory (except offheap and data-dirs), and are thus defaulted if not specified
    Set<String> mandatorySettings = DefaultSettings.getAll().keySet()
        .stream()
        .filter(key -> !key.equals(OFFHEAP_RESOURCES) && !key.equals(DATA_DIRS))
        .collect(Collectors.toSet());
    properties.forEach((key, value) -> {
      String property = getProperty(key.toString());
      if (mandatorySettings.contains(property) && value.toString().isEmpty()) {
        throw new MalformedConfigFileException("Missing value for property: " + property + " in config file: " + fileName +
            ". The following properties need to have values: " + mandatorySettings);
      }
    });
  }

  private static void ensureOnlyOneClusterName(Properties properties, String fileName) {
    Set<String> clusterNames = new HashSet<>();
    properties.forEach((key, value) -> clusterNames.add(getClusterName(key.toString())));

    if (clusterNames.size() != 1) {
      throw new MalformedConfigFileException("Expected only one cluster name in config file: " + fileName + ", but found: " + clusterNames);
    }
  }

  private static void ensureAllOptionsPresent(Properties properties, String fileName) {
    Map<NodeIdentifier, Set<String>> allNodesProperties = new HashMap<>();
    properties.forEach((key, value) -> {
      NodeIdentifier node = new NodeIdentifier(getStripeName(key.toString()), getNodeName(key.toString()));
      allNodesProperties.computeIfAbsent(node, k -> new HashSet<>());
      allNodesProperties.get(node).add(getProperty(key.toString()));
    });

    allNodesProperties.forEach((node, nodeProperties) -> {
      final HashSet<String> allOptions = new HashSet<>(ALL_VALID_OPTIONS);
      allOptions.removeAll(nodeProperties);
      if (allOptions.size() != 0) {
        throw new MalformedConfigFileException(node.getStripeName() + "." + node.getServerName() + " in file: " + fileName + " is missing the following properties: " + allOptions);
      }
    });
  }

  private static Properties loadProperties(File propertiesFile) {
    Properties props = new Properties();
    try (Reader in = new InputStreamReader(new FileInputStream(propertiesFile), StandardCharsets.UTF_8)) {
      props.load(in);
    } catch (FileNotFoundException e) {
      throw new UncheckedIOException(e);
    } catch (IOException e) {
      throw new MalformedConfigFileException("Failed to read config file: " + propertiesFile.getName(), e);
    }
    return props;
  }

  private static void ensureCorrectFieldCount(String key, String value, String fileName) {
    if (splitKey(key).length < 4) {
      throw new MalformedConfigFileException("Invalid line: " + key + "=" + value + " in config file: " + fileName + ". " +
          "Each line must be of the format: <cluster-name>.<stripe-name>.<node-name>.<property>=value");
    }
  }

  private static void ensureNoInvalidOptions(String key, String fileName) {
    final String property = getProperty(key);
    if (!ALL_VALID_OPTIONS.contains(property)) {
      throw new MalformedConfigFileException("Unrecognized property: " + property + " in config file: " + fileName);
    }
  }

  private static void ensureCorrectNodeName(String key, String value, String fileName) {
    if (getProperty(key).equals(NODE_NAME)) {
      if (!getNodeName(key).equals(value)) {
        throw new MalformedConfigFileException("Invalid line: " + key + "=" + value + " in config file: " + fileName + ". " +
            "Node name value should match the node name in the property. Expected: " + getNodeName(key) + ", found: " + value);
      }
    }
  }
}
