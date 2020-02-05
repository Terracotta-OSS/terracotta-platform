/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.cli.config_convertor;

import com.beust.jcommander.ParameterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.cli.command.CommandRepository;
import org.terracotta.dynamic_config.cli.command.CustomJCommander;
import org.terracotta.dynamic_config.cli.command.LocalMainCommand;
import org.terracotta.dynamic_config.cli.config_convertor.command.ConvertCommand;

import java.util.Arrays;
import java.util.HashSet;

import static java.lang.System.lineSeparator;

public class ConfigConvertorTool {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigConvertorTool.class);
  private static final LocalMainCommand MAIN = new LocalMainCommand();

  public static void main(String... args) {
    try {
      ConfigConvertorTool.start(args);
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
                new ConvertCommand()
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
    CustomJCommander jCommander = new CustomJCommander("config-convertor", commandRepository, MAIN);
    try {
      jCommander.parse(args);
    } catch (ParameterException e) {
      jCommander.printUsage();
      throw e;
    }
    return jCommander;
  }
}