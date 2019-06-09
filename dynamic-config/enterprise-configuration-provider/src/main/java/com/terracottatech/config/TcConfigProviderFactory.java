/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.config;

import com.terracotta.config.ConfigurationException;
import com.terracottatech.config.nomad.NomadConfigurationException;

class TcConfigProviderFactory {
  static TcConfigProvider init(CommandLineParser commandLineParser) throws ConfigurationException {
    if (commandLineParser.isConfigConsistencyMode()) {
      return new TcConfigFileProvider(commandLineParser.getConfig());
    } else {
      try {
        return new NomadRootTcConfigProvider(commandLineParser.getConfigurationRepo());
      } catch (NomadConfigurationException e) {
        throw new ConfigurationException("Exception while initializing Nomad Server", e);
      }
    }
  }
}