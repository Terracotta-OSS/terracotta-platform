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
import org.terracotta.common.struct.TimeUnit;
import org.terracotta.diagnostic.client.connection.ConcurrencySizing;
import org.terracotta.diagnostic.client.connection.ConcurrentDiagnosticServiceProvider;
import org.terracotta.diagnostic.client.connection.DiagnosticServiceProvider;
import org.terracotta.diagnostic.client.connection.MultiDiagnosticServiceProvider;
import org.terracotta.dynamic_config.api.json.DynamicConfigApiJsonModule;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.cli.command.CommandRepository;
import org.terracotta.dynamic_config.cli.command.CustomJCommander;
import org.terracotta.dynamic_config.cli.command.RemoteMainCommand;
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
import org.terracotta.dynamic_config.cli.config_tool.nomad.NomadManager;
import org.terracotta.dynamic_config.cli.config_tool.restart.RestartService;
import org.terracotta.dynamic_config.cli.config_tool.stop.StopService;
import org.terracotta.json.ObjectMapperFactory;
import org.terracotta.nomad.NomadEnvironment;
import org.terracotta.nomad.entity.client.NomadEntity;
import org.terracotta.nomad.entity.client.NomadEntityProvider;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

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
    final RemoteMainCommand mainCommand = new RemoteMainCommand();
    LOGGER.debug("Registering commands with CommandRepository");
    CommandRepository commandRepository = new CommandRepository();
    commandRepository.addAll(
        new HashSet<>(
            Arrays.asList(
                mainCommand,
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
    CustomJCommander jCommander = parseArguments(commandRepository, mainCommand, args);

    // Process arguments like '-v'
    mainCommand.validate();
    mainCommand.run();

    ConcurrencySizing concurrencySizing = new ConcurrencySizing();
    Duration connectionTimeout = Duration.ofMillis(mainCommand.getConnectionTimeout().getQuantity(TimeUnit.MILLISECONDS));
    Duration requestTimeout = Duration.ofMillis(mainCommand.getRequestTimeout().getQuantity(TimeUnit.MILLISECONDS));
    Duration entityOperationTimeout = Duration.ofMillis(mainCommand.getEntityOperationTimeout().getQuantity(TimeUnit.MILLISECONDS));
    Duration entityConnectionTimeout = Duration.ofMillis(mainCommand.getEntityConnectionTimeout().getQuantity(TimeUnit.MILLISECONDS));

    // create services
    ObjectMapperFactory objectMapperFactory = new ObjectMapperFactory().withModule(new DynamicConfigApiJsonModule());
    DiagnosticServiceProvider diagnosticServiceProvider = new DiagnosticServiceProvider("CONFIG-TOOL", connectionTimeout, requestTimeout, mainCommand.getSecurityRootDirectory(), objectMapperFactory);
    MultiDiagnosticServiceProvider multiDiagnosticServiceProvider = new ConcurrentDiagnosticServiceProvider(diagnosticServiceProvider, connectionTimeout, concurrencySizing);
    NomadEntityProvider nomadEntityProvider = new NomadEntityProvider(
        "CONFIG-TOOL",
        entityConnectionTimeout,
        // A long timeout is important here.
        // We need to block the call and wait for any return.
        // We cannot timeout shortly otherwise we won't know the outcome of the 2PC Nomad transaction in case of a failover.
        new NomadEntity.Settings().setRequestTimeout(entityOperationTimeout),
        mainCommand.getSecurityRootDirectory());
    NomadManager<NodeContext> nomadManager = new NomadManager<>(new NomadEnvironment(), multiDiagnosticServiceProvider, nomadEntityProvider);
    RestartService restartService = new RestartService(diagnosticServiceProvider, concurrencySizing);
    StopService stopService = new StopService(diagnosticServiceProvider, concurrencySizing);

    LOGGER.debug("Injecting services in CommandRepository");
    commandRepository.inject(diagnosticServiceProvider, multiDiagnosticServiceProvider, nomadManager, restartService, stopService, objectMapperFactory);

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

  private static CustomJCommander parseArguments(CommandRepository commandRepository, RemoteMainCommand mainCommand, String[] args) {
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

    try {
      jCommander.parse(args);
    } catch (ParameterException e) {
      jCommander.printUsage();
      throw e;
    }
    return jCommander;
  }
}
