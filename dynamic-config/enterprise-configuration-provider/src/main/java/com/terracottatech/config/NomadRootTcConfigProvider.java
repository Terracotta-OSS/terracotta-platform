/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.config;

import org.terracotta.config.TCConfigurationParser;
import org.terracotta.config.TcConfiguration;

import com.terracotta.config.ConfigurationException;
import com.terracottatech.config.nomad.NomadConfigurationException;
import com.terracottatech.config.nomad.NomadServerManager;
import com.terracottatech.config.nomad.NomadServerManagerImpl;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class NomadRootTcConfigProvider implements TcConfigProvider, Closeable {

  private final Path nomadRoot;
  private final NomadServerManager nomadServerManager;

  NomadRootTcConfigProvider(Path configurationRepo) throws NomadConfigurationException {
    this.nomadRoot = configurationRepo;
    this.nomadServerManager = new NomadServerManagerImpl();
    this.nomadServerManager.init(configurationRepo);
  }

  @Override
  public TcConfiguration provide() throws Exception {
    String configuration;
    try {
      configuration = this.nomadServerManager.getConfiguration();
    } catch (NomadConfigurationException e) {
      String errorMessage = String.format(
          "Unable to load the configuration from the configuration repo '%s', restart the server in '%s' mode using " +
          "command-line option '%s'",
          nomadRoot,
          "config-consistency",
          CommandLineParser.Opt.CONFIG_CONSISTENCY.longOption()
      );
      throw new ConfigurationException(errorMessage);
    }
    return TCConfigurationParser.parse(
        new ByteArrayInputStream(configuration.getBytes(StandardCharsets.UTF_8)),
        null,
        nomadRoot.toString()
    );
  }

  @Override
  public void close() {
    nomadServerManager.close();
  }
}
