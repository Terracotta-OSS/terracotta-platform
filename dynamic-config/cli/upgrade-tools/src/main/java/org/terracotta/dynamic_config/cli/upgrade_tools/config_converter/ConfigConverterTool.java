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
import org.terracotta.dynamic_config.cli.command.CustomJCommander;
import org.terracotta.dynamic_config.cli.command.JCommanderCommandRepository;
import org.terracotta.dynamic_config.cli.command.LocalMainCommand;
import org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.parsing.ConvertJCommanderCommand;
import org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.parsing.deprecated.DeprecatedConvertJCommanderCommand;

import java.util.Arrays;
import java.util.HashSet;

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
    LOGGER.debug("Registering commands with JCommanderCommandRepository");
    LocalMainCommand mainCommand = new LocalMainCommand();
    JCommanderCommandRepository commandRepository = new JCommanderCommandRepository();
    commandRepository.addAll(
        new HashSet<>(
            Arrays.asList(
                mainCommand,
                new ConvertJCommanderCommand(),
                new DeprecatedConvertJCommanderCommand()
            )
        )
    );

    LOGGER.debug("Parsing command-line arguments");
    CustomJCommander jCommander = parseArguments(commandRepository, args, mainCommand);

    // Process arguments like '-v'
    mainCommand.run();

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

  private static CustomJCommander parseArguments(JCommanderCommandRepository commandRepository, String[] args, LocalMainCommand mainCommand) {
    CustomJCommander jCommander = getCustomJCommander(commandRepository, mainCommand);
    try {
      jCommander.parse(args);
    } catch (ParameterException e) {
      String command = jCommander.getParsedCommand();
      if (command != null) {
        if (!command.contains("-deprecated")) {
          // Fallback to deprecated version
          try {
            for (int i = 0; i < args.length; ++i) {
              if (args[i].equals(command)) {
                args[i] = args[i].concat("-deprecated");
                break;
              }
            }
            // Create New JCommander object to avoid repeated main command error.
            CustomJCommander deprecatedJCommander = getCustomJCommander(commandRepository, mainCommand);
            deprecatedJCommander.parse(args);
            //success ?
            jCommander = deprecatedJCommander;
          } catch (ParameterException pe) {
            jCommander.printAskedCommandUsage(command);
            throw e;
          }
        }
      } else {
        jCommander.printUsage();
        throw e;
      }
    }
    return jCommander;
  }

  private static CustomJCommander getCustomJCommander(JCommanderCommandRepository commandRepository, LocalMainCommand mainCommand) {
    return new CustomJCommander("config-converter", commandRepository, mainCommand);
  }
}
