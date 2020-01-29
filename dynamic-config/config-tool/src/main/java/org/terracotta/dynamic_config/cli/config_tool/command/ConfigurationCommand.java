/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.cli.config_tool.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import org.terracotta.dynamic_config.api.model.Configuration;
import org.terracotta.dynamic_config.api.model.Operation;
import org.terracotta.dynamic_config.cli.converter.ConfigurationConverter;
import org.terracotta.dynamic_config.cli.converter.InetSocketAddressConverter;
import org.terracotta.dynamic_config.cli.converter.MultiConfigCommaSplitter;

import java.net.InetSocketAddress;
import java.util.List;

import static java.util.Objects.requireNonNull;

public abstract class ConfigurationCommand extends RemoteCommand {

  @Parameter(names = {"-s"}, description = "Node to connect to", required = true, converter = InetSocketAddressConverter.class)
  InetSocketAddress node;

  @Parameter(names = {"-c"}, description = "Configuration properties", splitter = MultiConfigCommaSplitter.class, required = true, converter = ConfigurationConverter.class)
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

    validateAddress(node);
  }
}
