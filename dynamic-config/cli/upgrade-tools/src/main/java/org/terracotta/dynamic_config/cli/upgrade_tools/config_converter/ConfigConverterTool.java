/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.dynamic_config.cli.upgrade_tools.config_converter;

import com.beust.jcommander.ParameterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.cli.api.command.Injector;
import org.terracotta.dynamic_config.cli.api.output.OutputService;
import org.terracotta.dynamic_config.cli.command.Command;
import org.terracotta.dynamic_config.cli.command.CustomJCommander;
import org.terracotta.dynamic_config.cli.command.LocalMainCommand;
import org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.parsing.ConvertCommand;
import org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.parsing.deprecated.DeprecatedConvertCommand;

import java.util.Collections;
import java.util.Map;

public class ConfigConverterTool {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigConverterTool.class);

  public static void main(String... args) {
    try {
      ConfigConverterTool.start(args);
    } catch (Exception e) {
      String message = e.getMessage();
      if (LOGGER.isDebugEnabled() || (message == null || message.isEmpty())) {
        LOGGER.error("Error:", e);
      } else {
        LOGGER.error("Error: {}", message);
      }
      System.exit(1);
    }
  }

  public static void start(String... args) {
    LOGGER.debug("Parsing command-line arguments");
    Map<String, Command> commands = Collections.singletonMap("convert", new ConvertCommand());
    Map<String, Command> depCommands = Collections.singletonMap("convert", new DeprecatedConvertCommand());
    CustomJCommander<LocalMainCommand> jCommander = parseArguments(commands, depCommands, args);

    // Process arguments like '-v'
    jCommander.getMainCommand().run();

    jCommander.getAskedCommand().map(command -> {
      // check for help
      if (command.isHelp()) {
        jCommander.printUsage();
        return true;
      }
      // validate and run the real command
      Injector.inject(command, Collections.singletonList(new OutputService()));
      command.run();
      return true;
    }).orElseGet(() -> {
      // If no command is provided, process help command
      jCommander.usage();
      return false;
    });
  }

  private static CustomJCommander<LocalMainCommand> parseArguments(Map<String, Command> commands, Map<String, Command> deprecatedCommands, String[] args) {
    LOGGER.debug("Attempting parse using regular commands");

    CustomJCommander<LocalMainCommand> jCommander = getCustomJCommander(commands, new LocalMainCommand());
    try {
      jCommander.parse(args);
      return jCommander;
    } catch (ParameterException e) {
      String command = jCommander.getParsedCommand();
      if (command != null) {
        // Fallback to deprecated version
        try {
          LOGGER.debug("Attempting parse using deprecated commands");

          // Create New JCommander object to avoid repeated main command error.
          CustomJCommander<LocalMainCommand> deprecatedJCommander = getCustomJCommander(deprecatedCommands, new LocalMainCommand());
          deprecatedJCommander.parse(args);
          // success ?
          return deprecatedJCommander;
        } catch (ParameterException pe) {
          // error ?
          // always display help file for new command
          jCommander.printAskedCommandUsage(command);
          throw e;
        }
      } else {
        jCommander.printUsage();
        throw e;
      }
    }
  }

  private static CustomJCommander<LocalMainCommand> getCustomJCommander(Map<String, Command> commands, LocalMainCommand mainCommand) {
    return new CustomJCommander<>("config-converter", commands, mainCommand);
  }
}
