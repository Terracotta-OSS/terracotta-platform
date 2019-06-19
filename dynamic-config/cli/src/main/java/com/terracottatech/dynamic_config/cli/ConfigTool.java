/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.terracottatech.diagnostic.client.connection.ConcurrencySizing;
import com.terracottatech.diagnostic.client.connection.DiagnosticServiceProvider;
import com.terracottatech.diagnostic.client.connection.MultiDiagnosticServiceConnectionFactory;
import com.terracottatech.dynamic_config.cli.command.MainCommand;
import com.terracottatech.dynamic_config.cli.connect.ConnectionDefaults;
import com.terracottatech.dynamic_config.cli.connect.DynamicConfigNodeAddressDiscovery;
import com.terracottatech.dynamic_config.cli.connect.NodeAddressDiscovery;
import com.terracottatech.dynamic_config.cli.manager.CommandManager;
import com.terracottatech.dynamic_config.cli.parse.CustomJCommander;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class ConfigTool {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigTool.class);

  public static void main(String[] args) {
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
      }
      System.exit(1);
    }
  }

  private void start(String[] args) {
    CommandManager commandManager = new CommandManager();
    MainCommand mainCommand = (MainCommand) commandManager.getCommand(MainCommand.NAME);
    mainCommand.process(null, null, null);

    String connectionTimeout = mainCommand.getConnectionTimeout();
    String requestTimeout = mainCommand.getRequestTimeout();
    String securityRootDirectory = mainCommand.getSecurityRootDirectory();
    int connTimeout = connectionTimeout == null ? ConnectionDefaults.DEFAULT_CONNECTION_TIMEOUT_MILLIS : Integer.parseInt(connectionTimeout);
    int reqTimeout = requestTimeout == null ? ConnectionDefaults.DEFAULT_DIAGNOSTIC_REQUEST_TIMEOUT_MILLIS : Integer.parseInt(requestTimeout);
    DiagnosticServiceProvider diagnosticServiceProvider = new DiagnosticServiceProvider("CONFIG-TOOL", reqTimeout, MILLISECONDS, securityRootDirectory);
    MultiDiagnosticServiceConnectionFactory connectionFactory = new MultiDiagnosticServiceConnectionFactory(diagnosticServiceProvider, connTimeout, MILLISECONDS, new ConcurrencySizing());
    NodeAddressDiscovery nodeAddressDiscovery = new DynamicConfigNodeAddressDiscovery(diagnosticServiceProvider, connTimeout, MILLISECONDS);

    JCommander jCommander = parseArguments(args, commandManager);

    // If no command is provided, process help command
    if (jCommander.getParsedCommand() == null) {
      jCommander.usage();
      return;
    }

    // Otherwise, process the real command
    try {
      commandManager.getCommand(jCommander.getParsedCommand()).process(jCommander, nodeAddressDiscovery, connectionFactory);
    } catch (ParameterException iax) {
      jCommander.usage(jCommander.getParsedCommand());
      throw iax;
    }
  }

  private JCommander parseArguments(String[] args, CommandManager commandManager) {
    JCommander jCommander = new CustomJCommander(commandManager.getCommand(MainCommand.NAME), commandManager);
    commandManager.getCommands().forEach(command -> {
      if (!MainCommand.NAME.equals(command.getName())) {
        jCommander.addCommand(command.getName(), command);
      }
    });

    try {
      jCommander.parse(args);
    } catch (ParameterException e) {
      String parsedCommand = jCommander.getParsedCommand();
      if (parsedCommand != null) {
        jCommander.usage(parsedCommand);
      }
      throw e;
    }
    return jCommander;
  }
}
