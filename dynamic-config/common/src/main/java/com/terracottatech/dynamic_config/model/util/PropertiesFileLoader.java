/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.model.util;

import com.terracottatech.dynamic_config.model.exception.MalformedConfigFileException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class PropertiesFileLoader {
  private final Path propertiesFile;

  public PropertiesFileLoader(Path propertiesFile) {
    this.propertiesFile = propertiesFile;
  }

  public Properties loadProperties() {
    Properties props = new Properties();
    try (InputStream in = Files.newInputStream(propertiesFile)) {
      props.load(in);
    } catch (IOException e) {
      throw new MalformedConfigFileException("Failed to read config file: %s. Make sure the file exists and is readable" + propertiesFile.getFileName(), e);
    }
    return props;
  }
}