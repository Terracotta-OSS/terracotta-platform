/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.config;

import com.terracottatech.dynamic_config.nomad.NomadBootstrapper;
import org.terracotta.config.TCConfigurationParser;
import org.terracotta.config.TcConfiguration;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class NomadRootTcConfigProvider implements TcConfigProvider {
  private final Path nomadRoot;

  NomadRootTcConfigProvider(Path configurationRepo) {
    this.nomadRoot = configurationRepo;
  }

  @Override
  public TcConfiguration provide() throws Exception {
    String configuration = NomadBootstrapper.getNomadServerManager().getConfiguration();
    return TCConfigurationParser.parse(
        new ByteArrayInputStream(configuration.getBytes(StandardCharsets.UTF_8)),
        null,
        nomadRoot.toString()
    );
  }
}
