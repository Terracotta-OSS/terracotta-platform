/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.config;

import com.terracottatech.dynamic_config.util.IParameterSubstitutor;

class TcConfigProviderFactory {
  static TcConfigProvider init(CommandLineParser commandLineParser, IParameterSubstitutor parameterSubstitutor) {
    if (commandLineParser.isConfigConsistencyMode()) {
      return new TcConfigFileProvider(commandLineParser.getConfig());
    } else {
      return new NomadTcConfigProvider(parameterSubstitutor);
    }
  }
}