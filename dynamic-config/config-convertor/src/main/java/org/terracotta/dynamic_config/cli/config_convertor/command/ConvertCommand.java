/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.cli.config_convertor.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.PathConverter;
import org.terracotta.dynamic_config.cli.command.Command;
import org.terracotta.dynamic_config.cli.command.Usage;
import org.terracotta.dynamic_config.cli.config_convertor.ConversionFormat;
import org.terracotta.dynamic_config.server.conversion.ConfigConvertor;
import org.terracotta.dynamic_config.server.conversion.ConfigPropertiesProcessor;
import org.terracotta.dynamic_config.server.conversion.PostConversionProcessor;
import org.terracotta.dynamic_config.server.conversion.ConfigRepoProcessor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static java.lang.System.lineSeparator;
import static org.terracotta.dynamic_config.cli.config_convertor.ConversionFormat.PROPERTIES;
import static org.terracotta.dynamic_config.cli.config_convertor.ConversionFormat.REPOSITORY;

@Parameters(commandNames = "convert", commandDescription = "Convert tc-config files to configuration repository format")
@Usage("convert -c <tc-config>,<tc-config>... -n <new-cluster-name> ( -l <license-file> [-t repository] | -t properties ) [-d <destination-dir>] [-f]")
public class ConvertCommand extends Command {
  @Parameter(names = {"-c"}, required = true, description = "An ordered list of tc-config files", converter = PathConverter.class)
  private List<Path> tcConfigFiles;

  @Parameter(names = {"-l"}, description = "Path to license file", converter = PathConverter.class)
  private Path licensePath;

  @Parameter(names = {"-d"}, description = "Destination directory to store converted config. Should not exist. Default: ${current-directory}/converted-configs", converter = PathConverter.class)
  private Path destinationDir = Paths.get(".").resolve("converted-configs");

  @Parameter(names = {"-n"}, required = true, description = "New cluster name")
  private String newClusterName;

  @Parameter(names = {"-t"}, description = "Conversion type (repository|properties). Default: repository", converter = ConversionFormat.FormatConverter.class)
  private ConversionFormat conversionFormat = REPOSITORY;

  @Parameter(names = {"-f"}, description = "Force a config conversion, ignoring warnings, if any. Default: false")
  private boolean force;

  @Override
  public void validate() {
    for (Path tcConfigFile : tcConfigFiles) {
      if (!Files.exists(tcConfigFile)) {
        throw new ParameterException("tc-config file: " + tcConfigFile + " not found");
      }
    }

    if (Files.exists(destinationDir)) {
      throw new ParameterException("Destination directory: " + destinationDir.toAbsolutePath().normalize() + " exists already. Please specify a non-existent directory");
    }

    if (licensePath != null && conversionFormat == PROPERTIES) {
      throw new ParameterException("Path to license file can only be provided for conversion to a config repository");
    }

    if (licensePath == null && conversionFormat == REPOSITORY) {
      throw new ParameterException("License file must be provided when converting to a config repository");
    }

    if (conversionFormat == REPOSITORY && !Files.exists(licensePath)) {
      throw new ParameterException("License file: " + licensePath + " not found");
    }
  }

  @Override
  public final void run() {
    if (conversionFormat == REPOSITORY) {
      PostConversionProcessor resultProcessor = new ConfigRepoProcessor(destinationDir);
      ConfigConvertor convertor = new ConfigConvertor(resultProcessor, force);
      convertor.processInput(newClusterName, tcConfigFiles.toArray(new Path[0]));
      if (licensePath != null) {
        try (Stream<Path> pathList = Files.list(destinationDir)) {
          pathList.forEach(repoPath -> {
            try {
              Path destLicenseDir = Files.createDirectories(repoPath.resolve("license")).resolve(licensePath.getFileName());
              Files.copy(licensePath, destLicenseDir);
            } catch (IOException e) {
              throw new UncheckedIOException(e);
            }
          });
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      }
      logger.info("Configuration repositories saved under: {}", destinationDir.toAbsolutePath().normalize());
    } else if (conversionFormat == PROPERTIES){
      PostConversionProcessor resultProcessor = new ConfigPropertiesProcessor(destinationDir, newClusterName);
      ConfigConvertor convertor = new ConfigConvertor(resultProcessor, force);
      convertor.processInput(newClusterName, tcConfigFiles.toArray(new Path[0]));
      logger.info("Configuration properties file saved under: {}", destinationDir.toAbsolutePath().normalize());
    } else {
      throw new AssertionError("Unexpected conversion format: " + conversionFormat);
    }

    logger.info("Command successful!" + lineSeparator());
  }
}
