/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli;

import com.beust.jcommander.ParameterException;
import com.terracottatech.diagnostic.client.connection.ConcurrencySizing;
import com.terracottatech.diagnostic.client.connection.DiagnosticServiceProvider;
import com.terracottatech.diagnostic.client.connection.MultiDiagnosticServiceConnectionFactory;
import com.terracottatech.dynamic_config.cli.service.command.ActivateCommand;
import com.terracottatech.dynamic_config.cli.service.command.AttachCommand;
import com.terracottatech.dynamic_config.cli.service.command.DetachCommand;
import com.terracottatech.dynamic_config.cli.service.command.DumpTopologyCommand;
import com.terracottatech.dynamic_config.cli.service.command.MainCommand;
import com.terracottatech.dynamic_config.cli.service.connect.DynamicConfigNodeAddressDiscovery;
import com.terracottatech.dynamic_config.cli.service.connect.NodeAddressDiscovery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class ConfigTool {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigTool.class);
  private static final MainCommand MAIN = new MainCommand();

  public static void main(String... args) {
    ConfigTool configTool = new ConfigTool();
    try {
      configTool.start(args);
    } catch (Exception e) {
      String message = e.getMessage();
      if (message != null && !message.isEmpty()) {
        String errorMessage = String.format("Error: %s%s", message, System.lineSeparator());
        if (LOGGER.isDebugEnabled()) {
          LOGGER.error(errorMessage, e);
        } else {
          LOGGER.error(errorMessage);
        }
      } else {
        LOGGER.error("Error: {}", Arrays.toString(e.getStackTrace()));
      }
      System.exit(1);
    }
  }

  private void start(String... args) {
    LOGGER.debug("Registering commands with CommandRepository");
    CommandRepository.addAll(
        new HashSet<>(
            Arrays.asList(
                MAIN,
                new ActivateCommand(),
                new AttachCommand(),
                new DetachCommand(),
                new DumpTopologyCommand()
            )
        )
    );

    LOGGER.debug("Parsing command-line arguments");
    CustomJCommander jCommander = parseArguments(args);

    // Process arguments like '-v'
    MAIN.run();

    // create services
    DiagnosticServiceProvider diagnosticServiceProvider = new DiagnosticServiceProvider("CONFIG-TOOL", MAIN.getRequestTimeoutMillis(), MILLISECONDS, MAIN.getSecurityRootDirectory());
    MultiDiagnosticServiceConnectionFactory connectionFactory = new MultiDiagnosticServiceConnectionFactory(diagnosticServiceProvider, MAIN.getConnectionTimeoutMillis(), MILLISECONDS, new ConcurrencySizing());
    NodeAddressDiscovery nodeAddressDiscovery = new DynamicConfigNodeAddressDiscovery(diagnosticServiceProvider, MAIN.getConnectionTimeoutMillis(), MILLISECONDS);

    LOGGER.debug("Injecting services in CommandRepository");
    CommandRepository.inject(diagnosticServiceProvider, connectionFactory, nodeAddressDiscovery);

    jCommander.getAskedCommand().map(command -> {
      // check for help
      if (command.isHelp()) {
        jCommander.printUsage();
        return true;
      }
      // validate the real command
      command.validate();
      // run the real command
      command.run();
      return true;
    }).orElseGet(() -> {
      // If no command is provided, process help command
      jCommander.usage();
      return false;
    });
  }

  private CustomJCommander parseArguments(String[] args) {
    CustomJCommander jCommander = new CustomJCommander(MAIN);
    try {
      jCommander.parse(args);
    } catch (ParameterException e) {
      jCommander.printUsage();
      throw e;
    }
    return jCommander;
  }
}
