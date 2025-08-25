/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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
package org.terracotta.dynamic_config.cli.config_tool;

import com.beust.jcommander.ParameterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.cli.api.command.Configuration;
import org.terracotta.dynamic_config.cli.api.command.Injector;
import org.terracotta.dynamic_config.cli.api.command.ServiceProvider;
import org.terracotta.dynamic_config.cli.api.output.ConsoleOutputService;
import org.terracotta.dynamic_config.cli.api.output.OutputService;
import org.terracotta.dynamic_config.cli.command.Command;
import org.terracotta.dynamic_config.cli.command.CustomJCommander;
import org.terracotta.dynamic_config.cli.config_tool.command.CommandProvider;
import org.terracotta.dynamic_config.cli.config_tool.parsing.RemoteMainCommand;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.lang.System.lineSeparator;

public class ConfigTool {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigTool.class);

  private final OutputService outputService;
  private final CommandProvider commandProvider;

  public ConfigTool() {
    this(new ConsoleOutputService());
  }

  public ConfigTool(OutputService outputService) {
    this(outputService, CommandProvider.get());
  }

  public ConfigTool(OutputService outputService, CommandProvider commandProvider) {
    this.outputService = outputService;
    this.commandProvider = commandProvider;
  }

  public void run(String... args) {
    LOGGER.debug("Parsing command-line arguments");
    CustomJCommander<RemoteMainCommand> jCommander = parseArguments(args);

    // Process arguments like '-v'
    RemoteMainCommand mainCommand = jCommander.getMainCommand();
    mainCommand.run();

    // create services
    Collection<Object> services = ServiceProvider.get().createServices(mainCommand.getConfiguration());

    jCommander.getAskedCommand().map(command -> {
      // check for help
      if (command.isHelp()) {
        jCommander.printUsage();
        return true;
      } else {
        LOGGER.debug("Injecting services in specified command");
        Injector.inject(command, services);
        // run the real command
        command.run();
        return true;
      }
    }).orElseGet(() -> {
      // If no command is provided, process help command
      jCommander.usage();
      return false;
    });
  }

  private CustomJCommander<RemoteMainCommand> parseArguments(String[] args) {
    LOGGER.debug("Attempting parse using regular commands");
    CustomJCommander<RemoteMainCommand> jCommander = getCustomJCommander(commandProvider.getCommands(), commandProvider.getMainCommand(new Configuration(outputService)));
    try {
      jCommander.parse(args);
    } catch (ParameterException e) {
      String command = jCommander.getParsedCommand();
      if (command != null) {
        // Fallback to deprecated version
        try {
          LOGGER.debug("Attempting parse using deprecated commands");
          // Create New JCommander object to avoid repeated main command error.
          CustomJCommander<RemoteMainCommand> deprecatedJCommander = getCustomJCommander(commandProvider.getDeprecatedCommands(), commandProvider.getMainCommand(new Configuration(outputService)));
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
    return jCommander;
  }

  public static void main(String... args) {
    try {
      new ConfigTool().run(args);
    } catch (Exception e) {
      String message = e.getMessage();
      if (message == null || message.isEmpty()) {
        // an unexpected error without message
        LOGGER.error("Internal error:", e);
      } else if (LOGGER.isDebugEnabled()) {
        // equivalent to verbose mode
        LOGGER.error("Error:", e);
      } else {
        // normal mode: only display message
        LOGGER.error("Error: {}", message);
      }
      System.exit(1);
    }
  }

  private static CustomJCommander<RemoteMainCommand> getCustomJCommander(Map<String, Command> commands, RemoteMainCommand mainCommand) {
    return new CustomJCommander<RemoteMainCommand>("config-tool", commands, mainCommand) {
      @Override
      public void appendDefinitions(StringBuilder out, String indent) {
        out.append(indent).append(lineSeparator()).append("Definitions:").append(lineSeparator());
        out.append(indent).append("    ").append("namespace").append(lineSeparator());
        Map<String, String> nameSpaces = new LinkedHashMap<>();
        nameSpaces.put("stripe.<stripeId>.node.<nodeId>", "to apply a change only on a specific node");
        nameSpaces.put("stripe.<stripeId>", "to apply a change only on a specific stripe");
        nameSpaces.put("'' (empty namespace)", "to apply a change only on all nodes of the cluster");

        int maxNamespaceLength = Integer.MIN_VALUE;
        for (String nameSpace : nameSpaces.keySet()) {
          if (nameSpace.length() > maxNamespaceLength) {
            maxNamespaceLength = nameSpace.length();
          }
        }

        for (Map.Entry<String, String> entry : nameSpaces.entrySet()) {
          String key = entry.getKey();
          String value = entry.getValue();
          out.append(indent).append("        ").append(key);
          for (int i = 0; i < maxNamespaceLength - key.length() + 4; i++) {
            out.append(" ");
          }
          out.append(value).append(lineSeparator());
        }
      }
    };
  }
}
