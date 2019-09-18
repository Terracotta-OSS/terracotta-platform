/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.model.validation;

import com.terracottatech.dynamic_config.model.exception.MalformedConfigFileException;
import com.terracottatech.utilities.Validator;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.nio.file.Path;
import java.util.Properties;

import static com.terracottatech.dynamic_config.util.ConfigFileParamsUtils.getSetting;
import static com.terracottatech.dynamic_config.util.ConfigFileParamsUtils.splitKey;
import static java.util.Objects.requireNonNull;

public class ConfigFileFormatValidator implements Validator {
  private final Path fileName;
  private final Properties properties;

  @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
  public ConfigFileFormatValidator(Path configFile, Properties properties) {
    this.fileName = requireNonNull(configFile.getFileName());
    this.properties = properties;
  }

  @Override
  public void validate() throws MalformedConfigFileException {
    properties.forEach((key, value) -> {
      ensureCorrectFieldCount(key.toString(), value.toString());
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

  private void ensureNoInvalidOptions(String key, String value) {
    try {
      getSetting(key);
    } catch (IllegalArgumentException e) {
      throw new MalformedConfigFileException(
          String.format(
              "Unrecognized setting in line: %s=%s in config fileName: %s",
              key,
              value,
              fileName
          )
      );
    }
  }
}
