/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.migration_tool;

import com.beust.jcommander.ParameterException;
import com.terracottatech.dynamic_config.cli.command.CommandRepository;
import com.terracottatech.dynamic_config.cli.command.CustomJCommander;
import com.terracottatech.dynamic_config.cli.command.MainCommand;
import com.terracottatech.dynamic_config.cli.migration_tool.command.MigrateCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;

import static java.lang.System.lineSeparator;

public class MigrationTool {
  private static final Logger LOGGER = LoggerFactory.getLogger(MigrationTool.class);
  private static final MainCommand MAIN = new MainCommand();

  public static void main(String... args) {
    try {
      MigrationTool.start(args);
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
                new MigrateCommand()
            )
        )
    );

    LOGGER.debug("Parsing command-line arguments");
    CustomJCommander jCommander = parseArguments(commandRepository, args);

    // Process arguments like '-v'
    MAIN.run();

    LOGGER.debug("Injecting services in CommandRepository");
    commandRepository.inject(); // no service injection yet

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
    CustomJCommander jCommander = new CustomJCommander("migration", commandRepository, MAIN);
    try {
      jCommander.parse(args);
    } catch (ParameterException e) {
      jCommander.printUsage();
      throw e;
    }
    return jCommander;
  }
}
