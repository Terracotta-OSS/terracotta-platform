/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.cli.migration_tool.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.PathConverter;
import org.terracotta.dynamic_config.cli.command.Command;
import org.terracotta.dynamic_config.cli.command.Usage;
import org.terracotta.dynamic_config.server.migration.MigrationImpl;
import org.terracotta.dynamic_config.server.migration.RepositoryStructureBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.lang.System.lineSeparator;

@Parameters(commandNames = "migrate", commandDescription = "Migrate tc-config files to configuration repository format")
@Usage("migrate -c <tc-config>,<tc-config>... -n <new-cluster-name> [-d <destination-dir>]")
public class MigrateCommand extends Command {
  @Parameter(names = {"-c"}, required = true, description = "tc-config files", converter = PathConverter.class)
  private List<Path> tcConfigFiles;

  @Parameter(names = {"-d"}, description = "Destination directory to store migrated repository. Default: current directory", converter = PathConverter.class)
  private Path destinationDir = Paths.get(".");

  @Parameter(names = {"-n"}, required = true, description = "New cluster name")
  private String newClusterName;

  @Override
  public void validate() {
    for (Path tcConfigFile : tcConfigFiles) {
      if (!Files.exists(tcConfigFile)) {
        throw new ParameterException("tc-config file: " + tcConfigFile + " not found");
      }
    }
  }

  @Override
  public final void run() {
    RepositoryStructureBuilder resultProcessor = new RepositoryStructureBuilder(destinationDir);
    MigrationImpl migration = new MigrationImpl(resultProcessor::process);
    migration.processInput(newClusterName, getMigrationStrings());
    logger.info("Configuration repositories saved under: {}", destinationDir.toAbsolutePath().normalize());
    logger.info("Command successful!" + lineSeparator());
  }

  private List<String> getMigrationStrings() {
    AtomicInteger counter = new AtomicInteger(0);
    return tcConfigFiles.stream().map(path -> counter.incrementAndGet() + "," + path.toString()).collect(Collectors.toList());
  }
}
