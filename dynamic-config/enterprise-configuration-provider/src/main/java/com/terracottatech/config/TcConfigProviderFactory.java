/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.config;

class TcConfigProviderFactory {
  static TcConfigProvider init(CommandLineParser commandLineParser) {
    return commandLineParser.getConfigurationRepositoryPath()
        .<TcConfigProvider>map(NomadRootTcConfigProvider::new)
        .orElseGet(() -> new TcConfigFileProvider(commandLineParser.getConfig()));
  }
}