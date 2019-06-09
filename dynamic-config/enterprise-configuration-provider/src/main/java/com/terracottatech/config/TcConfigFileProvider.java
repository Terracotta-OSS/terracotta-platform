/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.config;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.terracotta.config.TCConfigurationParser;
import org.terracotta.config.TcConfiguration;

import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.file.Files.newInputStream;
import static java.util.Collections.emptySet;
import static java.util.Objects.requireNonNull;

class TcConfigFileProvider implements TcConfigProvider {
  private final String configFile;

  TcConfigFileProvider(String configFile) {
    this.configFile = requireNonNull(configFile);
  }

  @Override
  @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "configFilePath cannot be null. Known findBugs bug")
  public TcConfiguration provide() throws Exception {
    Path configFilePath = Paths.get(configFile);
    return TCConfigurationParser.parse(newInputStream(configFilePath), emptySet(), configFilePath.getParent().toString());
  }
}
