/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.model.validation;

import com.terracottatech.dynamic_config.model.config.CommonOptions;
import com.terracottatech.dynamic_config.model.exception.MalformedConfigFileException;
import com.terracottatech.utilities.Validator;

import java.util.Properties;

import static com.terracottatech.dynamic_config.util.ConfigFileParamsUtils.getProperty;
import static com.terracottatech.dynamic_config.util.ConfigFileParamsUtils.splitKey;

public class ConfigFileValidator implements Validator {
  private final String fileName;
  private final Properties properties;

  public ConfigFileValidator(String fileName, Properties properties) {
    this.fileName = fileName;
    this.properties = properties;
  }

  @Override
  public void validate() throws MalformedConfigFileException {
    properties.forEach((key, value) -> {
      ensureCorrectFieldCount(key.toString(), value.toString());
      ensureNonEmptyValues(key.toString(), value.toString());
      ensureNoInvalidOptions(key.toString(), value.toString());
    });
  }

  private void ensureCorrectFieldCount(String key, String value) {
    if (splitKey(key).length != 5) {
      throw new MalformedConfigFileException(
          String.format(
              "Invalid line: %s=%s in config fileName: %s. Each line must be of the format: stripe.<index>.node.<index>.<property>=value",
              key,
              value,
              fileName
          )
      );
    }
  }

  private void ensureNonEmptyValues(String key, String value) {
    if (value.trim().isEmpty()) {
      throw new MalformedConfigFileException(
          String.format(
              "Missing value for key %s in config fileName: %s",
              key,
              fileName
          )
      );
    }
  }

  private void ensureNoInvalidOptions(String key, String value) {
    final String property = getProperty(key);
    if (!CommonOptions.getAllOptions().contains(property)) {
      throw new MalformedConfigFileException(
          String.format(
              "Unrecognized property: %s in line: %s=%s in config fileName: %s",
              property,
              key,
              value,
              fileName
          )
      );
    }
  }
}
