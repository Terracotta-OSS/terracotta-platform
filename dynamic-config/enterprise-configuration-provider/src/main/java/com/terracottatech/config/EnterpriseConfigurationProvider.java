/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.config;

import com.tc.classloader.OverrideService;
import com.tc.config.DefaultConfigurationProvider;
import com.terracotta.config.Configuration;
import com.terracotta.config.ConfigurationException;
import com.terracotta.config.ConfigurationProvider;
import org.terracotta.config.TcConfiguration;

import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@OverrideService("com.tc.config.DefaultConfigurationProvider")
public class EnterpriseConfigurationProvider implements ConfigurationProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(EnterpriseConfigurationProvider.class);

  private TcConfigProvider tcConfigProvider;
  private Configuration configuration;

  @Override
  public void initialize(List<String> configurationParams) throws ConfigurationException {
    try {
      CommandLineParser commandLineParser = getCommandLineParser(configurationParams);
      if (commandLineParser == null) return;

      tcConfigProvider = TcConfigProviderFactory.init(commandLineParser);
      configuration = getConfiguration(commandLineParser);
    } catch (Exception e) {
      throw new ConfigurationException("Unable to initialize EnterpriseConfigurationProvider with " + configurationParams, e);
    }
  }

  private CommandLineParser getCommandLineParser(List<String> configurationParams) throws ParseException, ConfigurationException {
    CommandLineParser commandLineParser;
    try {
      commandLineParser = new CommandLineParser(configurationParams);
    } catch (UnrecognizedOptionException e) {
      LOGGER.error("Encountered the following problem: " + e.getMessage() + ". Falling back to DefaultConfigurationProvider");
      // missing configuration repo option, fallback to OSS provider
      DefaultConfigurationProvider defaultConfigurationProvider = new DefaultConfigurationProvider();
      defaultConfigurationProvider.initialize(configurationParams);
      this.configuration = defaultConfigurationProvider.getConfiguration();
      return null;
    }
    return commandLineParser;
  }

  private Configuration getConfiguration(CommandLineParser commandLineParser) throws Exception {
    TcConfiguration tcConfiguration = tcConfigProvider.provide();
    LOGGER.info("Startup configuration of the node: \n\n{}", tcConfiguration);
    return new TcConfigurationWrapper(tcConfiguration, commandLineParser.isConfigConsistencyMode());
  }

  @Override
  public Configuration getConfiguration() {
    return configuration;
  }

  @Override
  public String getConfigurationParamsDescription() {
    return CommandLineParser.getConfigurationParamsDescription();
  }

  @Override
  public void close() {
    tcConfigProvider.close();
  }
}
