/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.server.startup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.api.service.IParameterSubstitutor;
import org.terracotta.dynamic_config.server.parsing.Options;

import java.nio.file.Path;
import java.util.Optional;

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
  public boolean startNode() {
    Path repositoryDir = startupManager.getOrDefaultRepositoryDir(options.getNodeRepositoryDir());
    Optional<String> nodeName = startupManager.findNodeName(repositoryDir);
    if (nodeName.isPresent()) {
      return startupManager.startUsingConfigRepo(repositoryDir, nodeName.get());
    }

    LOGGER.info("Did not find config repository at: " + parameterSubstitutor.substitute(repositoryDir));
    // Couldn't start node - pass the responsibility to the next starter
    return nextStarter.startNode();
  }
}
