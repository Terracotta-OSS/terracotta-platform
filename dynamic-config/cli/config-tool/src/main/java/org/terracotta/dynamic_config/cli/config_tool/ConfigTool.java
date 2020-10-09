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
package org.terracotta.dynamic_config.cli.config_tool;

import com.beust.jcommander.ParameterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.cli.command.CustomJCommander;
import org.terracotta.dynamic_config.cli.command.Injector;
import org.terracotta.dynamic_config.cli.command.JCommanderCommand;
import org.terracotta.dynamic_config.cli.command.JCommanderCommandRepository;
import org.terracotta.dynamic_config.cli.command.RemoteMainCommand;
import org.terracotta.dynamic_config.cli.config_tool.command.JCommanderCommandProvider;
import org.terracotta.dynamic_config.cli.config_tool.command.ServiceProvider;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static java.lang.System.lineSeparator;

public class ConfigTool {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigTool.class);

  public static void main(String... args) {
    try {
      ConfigTool.start(args);
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

  public static void start(String... args) {
    JCommanderCommandProvider commandProvider = JCommanderCommandProvider.get();
    final RemoteMainCommand mainCommand = new RemoteMainCommand();
    LOGGER.debug("Registering commands with JCommanderCommandRepository");
    JCommanderCommandRepository commandRepository = new JCommanderCommandRepository();
    Set<JCommanderCommand> commands = commandProvider.getCommands();
    commands.add(mainCommand);
    commandRepository.addAll(commands);

    LOGGER.debug("Parsing command-line arguments");
    CustomJCommander jCommander = parseArguments(commandRepository, mainCommand, args);

    // Process arguments like '-v'
    mainCommand.validate();
    mainCommand.run();

    // create services
    Collection<Object> services = ServiceProvider.get().createServices(mainCommand);

    jCommander.getAskedCommand().map(command -> {
      // check for help
      if (command.isHelp()) {
        jCommander.printUsage();
        return true;
      } else {
        LOGGER.debug("Injecting services in specified command");
        Injector.inject(command.getCommand(), services);
        // validate the real command
        command.validate();
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

  private static CustomJCommander parseArguments(JCommanderCommandRepository commandRepository, RemoteMainCommand mainCommand, String[] args) {
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
            jCommander = getCustomJCommander(commandRepository, mainCommand);
            jCommander.parse(args);
          } catch (ParameterException pe) {
            jCommander.printAskedCommandUsage(command);
            throw pe;
          }
        }
      } else {
        jCommander.printUsage();
        throw e;
      }
    }
    return jCommander;
  }

  private static CustomJCommander getCustomJCommander(JCommanderCommandRepository commandRepository, RemoteMainCommand mainCommand) {
    CustomJCommander jCommander = new CustomJCommander("config-tool", commandRepository, mainCommand) {
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
    return jCommander;
  }
}
