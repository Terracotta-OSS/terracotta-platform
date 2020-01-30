/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.cli.config_tool;

import com.beust.jcommander.ParameterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.common.struct.TimeUnit;
import org.terracotta.diagnostic.client.connection.ConcurrencySizing;
import org.terracotta.diagnostic.client.connection.ConcurrentDiagnosticServiceProvider;
import org.terracotta.diagnostic.client.connection.DiagnosticServiceProvider;
import org.terracotta.diagnostic.client.connection.MultiDiagnosticServiceProvider;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.cli.command.CommandRepository;
import org.terracotta.dynamic_config.cli.command.CustomJCommander;
import org.terracotta.dynamic_config.cli.command.MainCommand;
import org.terracotta.dynamic_config.cli.config_tool.command.ActivateCommand;
import org.terracotta.dynamic_config.cli.config_tool.command.AttachCommand;
import org.terracotta.dynamic_config.cli.config_tool.command.DetachCommand;
import org.terracotta.dynamic_config.cli.config_tool.command.DiagnosticCommand;
import org.terracotta.dynamic_config.cli.config_tool.command.ExportCommand;
import org.terracotta.dynamic_config.cli.config_tool.command.GetCommand;
import org.terracotta.dynamic_config.cli.config_tool.command.ImportCommand;
import org.terracotta.dynamic_config.cli.config_tool.command.LogCommand;
import org.terracotta.dynamic_config.cli.config_tool.command.RepairCommand;
import org.terracotta.dynamic_config.cli.config_tool.command.SetCommand;
import org.terracotta.dynamic_config.cli.config_tool.command.UnsetCommand;
import org.terracotta.dynamic_config.cli.config_tool.nomad.NomadClientFactory;
import org.terracotta.dynamic_config.cli.config_tool.nomad.NomadManager;
import org.terracotta.dynamic_config.cli.config_tool.restart.RestartService;
import org.terracotta.nomad.NomadEnvironment;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;

import static java.lang.System.lineSeparator;

public class ConfigTool {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigTool.class);
  private static final MainCommand MAIN = new MainCommand();

  public static void main(String... args) {
    try {
      ConfigTool.start(args);
    } catch (Exception e) {
      String message = e.getMessage();
      if (message != null && !message.isEmpty()) {
        String errorMessage = String.format("%sError:%s%s%s", lineSeparator(), lineSeparator(), message, lineSeparator());
        if (LOGGER.isDebugEnabled()) {
          LOGGER.error("{}Error:", lineSeparator(), e); // do not output e.getMassage() because it duplicates the output
        } else {
          LOGGER.error(errorMessage);
        }
      } else {
        LOGGER.error("{}Internal error:", lineSeparator(), e);
      }
      System.exit(1);
    }
  }

  public static void start(String... args) {
    LOGGER.debug("Registering commands with CommandRepository");
    CommandRepository commandRepository = new CommandRepository();
    commandRepository.addAll(
        new HashSet<>(
            Arrays.asList(
                MAIN,
                new ActivateCommand(),
                new AttachCommand(),
                new DetachCommand(),
                new ImportCommand(),
                new ExportCommand(),
                new GetCommand(),
                new SetCommand(),
                new UnsetCommand(),
                new DiagnosticCommand(),
                new RepairCommand(),
                new LogCommand()
            )
        )
    );

    LOGGER.debug("Parsing command-line arguments");
    CustomJCommander jCommander = parseArguments(commandRepository, args);

    // Process arguments like '-v'
    MAIN.run();

    ConcurrencySizing concurrencySizing = new ConcurrencySizing();
    Duration connectionTimeout = Duration.ofMillis(MAIN.getConnectionTimeout().getQuantity(TimeUnit.MILLISECONDS));
    Duration requestTimeout = Duration.ofMillis(MAIN.getRequestTimeout().getQuantity(TimeUnit.MILLISECONDS));

    // create services
    DiagnosticServiceProvider diagnosticServiceProvider = new DiagnosticServiceProvider("CONFIG-TOOL", connectionTimeout, requestTimeout, MAIN.getSecurityRootDirectory());
    MultiDiagnosticServiceProvider multiDiagnosticServiceProvider = new ConcurrentDiagnosticServiceProvider(diagnosticServiceProvider, connectionTimeout, concurrencySizing);
    NomadManager<NodeContext> nomadManager = new NomadManager<>(new NomadClientFactory<>(multiDiagnosticServiceProvider, new NomadEnvironment()));
    RestartService restartService = new RestartService(diagnosticServiceProvider, concurrencySizing);

    LOGGER.debug("Injecting services in CommandRepository");
    commandRepository.inject(diagnosticServiceProvider, multiDiagnosticServiceProvider, nomadManager, restartService);

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

  private static CustomJCommander parseArguments(CommandRepository commandRepository, String[] args) {
    CustomJCommander jCommander = new CustomJCommander("config-tool", commandRepository, MAIN);
    try {
      jCommander.parse(args);
    } catch (ParameterException e) {
      jCommander.printUsage();
      throw e;
    }
    return jCommander;
  }
}
