/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.service.command;

import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.terracottatech.dynamic_config.cli.common.Usage;
import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.Configuration;
import com.terracottatech.dynamic_config.model.Operation;

import java.util.Properties;
import java.util.stream.Collectors;

@Parameters(commandNames = "get", commandDescription = "Display properties of nodes")
@Usage("get -s HOST -c NAMESPACE1.PROPERTY1,NAMESPACE2.PROPERTY2,...")
public class GetCommand extends ConfigurationCommand {
  public GetCommand() {
    super(Operation.GET);
  }

  @Override
  public void run() {
    Cluster cluster = getRemoteTopology(node);
    Properties properties = new Properties();
    // we put both expanded and non expanded properties
    // and we will filter depending on what the user wanted
    properties.putAll(cluster.toProperties(false));
    properties.putAll(cluster.toProperties(true));
    // we filter the properties the user wants based on his input
    String output = properties.entrySet()
        .stream()
        .filter(e -> acceptKey(e.getKey().toString()))
        .map(e -> e.getKey() + "=" + e.getValue())
        .sorted()
        .reduce((result, line) -> result + System.lineSeparator() + line)
        .orElseThrow(() -> new ParameterException("No configuration found for: " + configurations.stream().map(Configuration::toString).collect(Collectors.joining(", "))));
    logger.info(output);
  }

  private boolean acceptKey(String key) {
    return configurations.stream().anyMatch(configuration -> configuration.matchConfigPropertyKey(key));
  }
}
