/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.service.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.terracottatech.dynamic_config.cli.common.ConfigurationConverter;
import com.terracottatech.dynamic_config.cli.common.InetSocketAddressConverter;
import com.terracottatech.dynamic_config.cli.common.MultiConfigCommaSplitter;
import com.terracottatech.dynamic_config.model.Configuration;
import com.terracottatech.dynamic_config.model.Operation;

import java.net.InetSocketAddress;
import java.util.List;

import static java.util.Objects.requireNonNull;

public abstract class ConfigurationCommand extends RemoteCommand {

  @Parameter(names = {"-s"}, description = "Node to connect to", required = true, converter = InetSocketAddressConverter.class)
  InetSocketAddress node;

  @Parameter(names = {"-c"}, description = "Config properties to be set", splitter = MultiConfigCommaSplitter.class, required = true, converter = ConfigurationConverter.class)
  List<Configuration> configurations;

  protected final Operation operation;

  protected ConfigurationCommand(Operation operation) {
    this.operation = operation;
  }

  @Override
  public void validate() {
    requireNonNull(node);
    requireNonNull(configurations);

    // validate all configurations passes on CLI
    for (Configuration configuration : configurations) {
      configuration.validate(operation);
    }

    // once valid, check for duplicates
    for (int i = 0; i < configurations.size(); i++) {
      Configuration first = configurations.get(i);
      for (int j = i + 1; j < configurations.size(); j++) {
        Configuration second = configurations.get(j);
        if (second.duplicates(first)) {
          throw new ParameterException("Duplicate configurations found: " + first + " and " + second);
        }
      }
    }

    ensureAddressWithinCluster(node);
  }
}
