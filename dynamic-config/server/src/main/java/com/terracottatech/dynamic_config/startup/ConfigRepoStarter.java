/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.startup;

import com.terracottatech.dynamic_config.parsing.Options;
import com.terracottatech.dynamic_config.util.IParameterSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class ConfigRepoStarter implements NodeStarter {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigRepoStarter.class);

  private final Options options;
  private final NodeStarter nextStarter;
  private final StartupManager startupManager;
  private final IParameterSubstitutor parameterSubstitutor;

  ConfigRepoStarter(Options options, StartupManager startupManager, NodeStarter nextStarter, IParameterSubstitutor parameterSubstitutor) {
    this.options = options;
    this.nextStarter = nextStarter;
    this.startupManager = startupManager;
    this.parameterSubstitutor = parameterSubstitutor;
  }

  @Override
  public void startNode() {
    Path repositoryDir = startupManager.getOrDefaultRepositoryDir(options.getNodeRepositoryDir());
    startupManager.findNodeName(repositoryDir).ifPresent(nodeName -> startupManager.startUsingConfigRepo(repositoryDir, nodeName));

    LOGGER.info("Did not find config repository at: " + parameterSubstitutor.substitute(repositoryDir));
    // Couldn't start node - pass the responsibility to the next starter
    nextStarter.startNode();
  }
}
