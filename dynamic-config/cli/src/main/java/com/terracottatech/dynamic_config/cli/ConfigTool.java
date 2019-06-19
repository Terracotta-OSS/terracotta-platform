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
import com.terracottatech.dynamic_config.cli.connect.DynamicConfigNodeAddressDiscovery;
import com.terracottatech.dynamic_config.cli.connect.NodeAddressDiscovery;
import com.terracottatech.dynamic_config.cli.manager.CommandManager;
import com.terracottatech.dynamic_config.cli.parse.CustomJCommander;

import static java.util.concurrent.TimeUnit.SECONDS;

public class ConfigTool {
  public static void main(String[] args) {
    ConfigTool configTool = new ConfigTool();
    configTool.start(args);
  }

  private void start(String[] args) {
    DiagnosticServiceProvider diagnosticServiceProvider = new DiagnosticServiceProvider("CONFIG-TOOL", 10, SECONDS, null);
    MultiDiagnosticServiceConnectionFactory connectionFactory = new MultiDiagnosticServiceConnectionFactory(diagnosticServiceProvider, 10, SECONDS, new ConcurrencySizing());
    NodeAddressDiscovery nodeAddressDiscovery = new DynamicConfigNodeAddressDiscovery(diagnosticServiceProvider, 10, SECONDS);

    CommandManager commandManager = new CommandManager(nodeAddressDiscovery, connectionFactory);
    commandManager.getCommand(MainCommand.NAME).run();
    JCommander jCommander = this.parseArguments(args, commandManager);

    // If no command is provided, process help command
    if (jCommander.getParsedCommand() == null) {
      jCommander.usage(); //intentionally bypassing logger
      return;
    }

    // Otherwise, process the real command
    try {
      commandManager.getCommand(jCommander.getParsedCommand()).run();
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
