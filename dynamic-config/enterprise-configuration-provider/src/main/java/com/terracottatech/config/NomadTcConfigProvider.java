/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.config;

import com.terracottatech.dynamic_config.nomad.NomadBootstrapper;
import org.terracotta.config.TCConfigurationParser;
import org.terracotta.config.TcConfiguration;

public class NomadTcConfigProvider implements TcConfigProvider {
  @Override
  public TcConfiguration provide() throws Exception {
    String configuration = NomadBootstrapper.getNomadServerManager().getConfiguration();
    // TCConfigurationParser substitutes values for platform parameters, so anything known to platform needn't be substituted before this
    return TCConfigurationParser.parse(configuration);
  }
}
