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
import com.terracottatech.dynamic_config.handler.ConfigChangeHandlerManager;
import com.terracottatech.dynamic_config.model.Setting;
import com.terracottatech.dynamic_config.nomad.NomadBootstrapper;
import com.terracottatech.dynamic_config.service.ConfigChangeHandlerManagerImpl;
import com.terracottatech.dynamic_config.util.IParameterSubstitutor;
import com.terracottatech.dynamic_config.util.ParameterSubstitutor;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.config.TcConfiguration;

import java.nio.file.Paths;
import java.util.List;

import static com.terracottatech.dynamic_config.nomad.NomadBootstrapper.bootstrap;

@OverrideService("com.tc.config.DefaultConfigurationProvider")
public class EnterpriseConfigurationProvider implements ConfigurationProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(EnterpriseConfigurationProvider.class);

  private CommandLineParser cliParser;
  private Configuration configuration;
  private IParameterSubstitutor parameterSubstitutor = new ParameterSubstitutor();
  private ConfigChangeHandlerManager configChangeHandlerManager = new ConfigChangeHandlerManagerImpl();
  private volatile ConfigurationSyncManager configurationSyncManager;

  @Override
  public void initialize(List<String> configurationParams) throws ConfigurationException {
    try {
      cliParser = getCommandLineParser(configurationParams);
      if (cliParser == null) return;

      bootstrapNomad();
      configuration = createConfiguration();
    } catch (Exception e) {
      throw new ConfigurationException("Unable to initialize EnterpriseConfigurationProvider with " + configurationParams, e);
    }
  }

  private void bootstrapNomad() {
    String configRepository = cliParser.getConfigRepository() == null ? Setting.NODE_REPOSITORY_DIR.getDefaultValue() : cliParser.getConfigRepository();
    NomadBootstrapper.NomadServerManager nomadServerManager =
        bootstrap(Paths.get(configRepository), parameterSubstitutor, configChangeHandlerManager, cliParser.getNodeName());
    this.configurationSyncManager = new ConfigurationSyncManager(nomadServerManager.getNomadServer());
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

  private Configuration createConfiguration() throws Exception {
    TcConfigProvider tcConfigProvider = TcConfigProviderFactory.init(cliParser, parameterSubstitutor);
    TcConfiguration tcConfiguration = tcConfigProvider.provide();
    LOGGER.info("Startup configuration of the node: \n\n{}", tcConfiguration);
    return new TcConfigurationWrapper(tcConfiguration, cliParser.isConfigConsistencyMode());
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
  public void sync(byte[] configuration) {
    if (configurationSyncManager != null) {
      configurationSyncManager.sync(configuration);
    }
  }

  @Override
  public void close() {
    // Do nothing
  }
}
