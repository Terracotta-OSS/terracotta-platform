/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.model.validation;

import com.terracottatech.dynamic_config.model.config.CommonOptions;
import com.terracottatech.dynamic_config.model.exception.MalformedConfigFileException;
import com.terracottatech.utilities.Tuple2;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import static com.terracottatech.dynamic_config.model.util.ConfigFileParamsUtils.getNodeName;
import static com.terracottatech.dynamic_config.model.util.ConfigFileParamsUtils.getProperty;
import static com.terracottatech.dynamic_config.model.util.ConfigFileParamsUtils.getStripeId;
import static com.terracottatech.dynamic_config.model.util.ConfigFileParamsUtils.splitKey;

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
      ensureNonEmptyValues(key.toString(), value.toString(), fileName);
      ensureNoInvalidOptions(key.toString(), value.toString(), fileName);
    });
    invokeNodeParamsValidation(properties);
  }

  private static void ensureCorrectFieldCount(String key, String value, String fileName) {
    if (splitKey(key).length != 5) {
      throw new MalformedConfigFileException(
          String.format(
              "Invalid line: %s=%s in config file: %s. Each line must be of the format: stripe.<index>.node.<index>.<property>=value",
              key,
              value,
              fileName
          )
      );
    }
  }

  private static void ensureNonEmptyValues(String key, String value, String fileName) {
    if (value.trim().isEmpty()) {
      throw new MalformedConfigFileException(
          String.format(
              "Missing value for key %s in config file: %s",
              key,
              fileName
          )
      );
    }
  }

  private static void ensureNoInvalidOptions(String key, String value, String fileName) {
    final String property = getProperty(key);
    if (!ALL_VALID_OPTIONS.contains(property)) {
      throw new MalformedConfigFileException(
          String.format(
              "Unrecognized property: %s in line: %s=%s in config file: %s",
              property,
              key,
              value,
              fileName
          )
      );
    }
  }

  private static void invokeNodeParamsValidation(Properties properties) {
    Map<Tuple2<Integer, String>, Map<String, String>> nodeParamValueMap = properties.entrySet().stream()
        .collect(
            Collectors.groupingBy(
                entry -> Tuple2.tuple2(getStripeId(entry.getKey().toString()), getNodeName(entry.getKey().toString())),
                Collectors.toMap(entry -> getProperty(entry.getKey().toString()), entry -> entry.getValue().toString())
            )
        );
    nodeParamValueMap.forEach((nodeIdentifier, map) -> NodeParamsValidator.validate(map));
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
}
