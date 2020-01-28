/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.server.config_provider;

import com.terracottatech.dynamic_config.api.service.IParameterSubstitutor;

class TcConfigProviderFactory {
  static TcConfigProvider init(CommandLineParser commandLineParser, IParameterSubstitutor parameterSubstitutor) {
    if (commandLineParser.isConfigConsistencyMode()) {
      return new TcConfigFileProvider(commandLineParser.getConfig());
    } else {
      return new NomadTcConfigProvider(parameterSubstitutor);
    }
  }
}