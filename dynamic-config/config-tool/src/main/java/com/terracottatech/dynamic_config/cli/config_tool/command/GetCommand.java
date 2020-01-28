/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.config_tool.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.BooleanConverter;
import com.terracottatech.dynamic_config.api.model.Cluster;
import com.terracottatech.dynamic_config.api.model.Configuration;
import com.terracottatech.dynamic_config.api.model.Operation;
import com.terracottatech.dynamic_config.cli.command.Usage;

import java.util.Properties;
import java.util.stream.Collectors;

@Parameters(commandNames = "get", commandDescription = "Read configuration properties")
@Usage("get -s <hostname[:port]> [-r] -c <[namespace:]property>,<[namespace:]property>...")
public class GetCommand extends ConfigurationCommand {

  @Parameter(names = {"-r"}, description = "Read the properties from the current runtime configuration instead of reading them from the last configuration saved on disk", converter = BooleanConverter.class)
  private boolean wantsRuntimeConfig;

  public GetCommand() {
    super(Operation.GET);
  }

  @Override
  public void run() {
    Cluster cluster = wantsRuntimeConfig ? getRuntimeCluster(node) : getUpcomingCluster(node);
    Properties properties = new Properties();
    // we put both expanded and non expanded properties
    // and we will filter depending on what the user wanted
    properties.putAll(cluster.toProperties(false, true));
    properties.putAll(cluster.toProperties(true, true));
    // we filter the properties the user wants based on his input
    String output = properties.entrySet()
        .stream()
        .filter(e -> acceptKey(e.getKey().toString()))
        .map(e -> e.getKey() + "=" + e.getValue())
        .sorted()
        .reduce((result, line) -> result + System.lineSeparator() + line)
        .orElseThrow(() -> new ParameterException("No configuration found for: " + configurations.stream().map(Configuration::toString).collect(Collectors.joining(", "))));
    logger.info(output + System.lineSeparator());
  }

  private boolean acceptKey(String key) {
    return configurations.stream().anyMatch(configuration -> configuration.matchConfigPropertyKey(key));
  }
}
