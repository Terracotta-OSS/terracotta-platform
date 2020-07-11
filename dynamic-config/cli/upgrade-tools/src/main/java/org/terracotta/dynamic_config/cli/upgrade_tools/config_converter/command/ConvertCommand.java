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
package org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.PathConverter;
import org.terracotta.dynamic_config.cli.command.Command;
import org.terracotta.dynamic_config.cli.command.DeprecatedParameter;
import org.terracotta.dynamic_config.cli.command.DeprecatedUsage;
import org.terracotta.dynamic_config.cli.command.Usage;
import org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.ConfigConverter;
import org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.ConfigPropertiesProcessor;
import org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.ConfigRepoProcessor;
import org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.ConversionFormat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static java.lang.System.lineSeparator;
import static java.nio.file.Files.isDirectory;
import static org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.ConversionFormat.DIRECTORY;
import static org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.ConversionFormat.PROPERTIES;

@Parameters(commandNames = "convert", commandDescription = "Convert tc-config files to configuration directory format")
@DeprecatedUsage("convert -c <tc-config>,<tc-config>... ( -t directory [-l <license-file>] -n <new-cluster-name> | -t properties [-n <new-cluster-name>]) [-d <destination-dir>] [-f]")
@Usage("convert -tc-config <tc-config>,<tc-config>... ( -conversion-type directory [-license-file <license-file>] -cluster-name <new-cluster-name>" +
    " | -conversion-type properties [-cluster-name <new-cluster-name>]) [-destination-dir <destination-dir>] [-f]")
public class ConvertCommand extends Command {
  @DeprecatedParameter(names = "-c", required = true, description = "An ordered list of tc-config files", converter = PathConverter.class)
  @Parameter(names = "-tc-config", required = true, description = "An ordered list of tc-config files", converter = PathConverter.class)
  private List<Path> tcConfigFiles;

  @DeprecatedParameter(names = "-l", description = "Path to license file", converter = PathConverter.class)
  @Parameter(names = "-license-file", description = "Path to license file", converter = PathConverter.class)
  private Path licensePath;

  @DeprecatedParameter(names = "-d", description = "Destination directory to store converted config. Should not exist. Default: ${CURRENT_DIR}/converted-configs", converter = PathConverter.class)
  @Parameter(names = "-destination-dir", description = "Destination directory to store converted config. Should not exist. Default: ${CURRENT_DIR}/converted-configs", converter = PathConverter.class)
  private Path destinationDir = Paths.get(".").resolve("converted-configs");

  @DeprecatedParameter(names = "-n", description = "New cluster name")
  @Parameter(names = "-cluster-name", description = "New cluster name")
  private String newClusterName;

  @DeprecatedParameter(names = "-t", description = "Conversion type (directory|properties). Default: directory", converter = ConversionFormat.FormatConverter.class)
  @Parameter(names = "-conversion-type", description = "Conversion type (directory|properties). Default: directory", converter = ConversionFormat.FormatConverter.class)
  private ConversionFormat conversionFormat = DIRECTORY;

  @DeprecatedParameter(names = "-f", description = "Force a config conversion, ignoring warnings, if any. Default: false")
  @Parameter(names = "-force", description = "Force a config conversion, ignoring warnings, if any. Default: false")
  private boolean force;

  @Override
  public void validate() {
    for (Path tcConfigFile : tcConfigFiles) {
      if (!tcConfigFile.toFile().exists()) {
        throw new ParameterException("tc-config file: " + tcConfigFile + " not found");
      }
    }

    if (destinationDir.toFile().exists()) {
      throw new ParameterException("Destination directory: " + destinationDir.toAbsolutePath().normalize() + " exists already. Please specify a non-existent directory");
    }

    if (licensePath != null && conversionFormat == PROPERTIES) {
      throw new ParameterException("Path to license file can only be provided for conversion into a configuration directory");
    }

    if (newClusterName == null && conversionFormat == DIRECTORY) {
      throw new ParameterException("Cluster name is required for conversion into a configuration directory");
    }

    if (licensePath != null && !licensePath.toFile().exists()) {
      throw new ParameterException("License file: " + licensePath + " not found");
    }
  }

  @Override
  public final void run() {
    if (conversionFormat == DIRECTORY) {
      ConfigRepoProcessor resultProcessor = new ConfigRepoProcessor(destinationDir);
      ConfigConverter converter = new ConfigConverter(resultProcessor::process, force);
      converter.processInput(newClusterName, tcConfigFiles.toArray(new Path[0]));

      if (licensePath != null) {
        try (Stream<Path> allLicenseDirs = Files.find(destinationDir, 3, (path, attrs) -> path.getFileName().toString().equals("license") && isDirectory(path))) {
          allLicenseDirs.forEach(licenseDir -> {
            try {
              Path destLicenseFile = licenseDir.resolve(licensePath.getFileName());
              org.terracotta.utilities.io.Files.copy(licensePath, destLicenseFile);
            } catch (IOException e) {
              throw new UncheckedIOException(e);
            }
          });
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      }
      logger.info("Configuration directories saved under: {}", destinationDir.toAbsolutePath().normalize());

    } else if (conversionFormat == PROPERTIES) {
      ConfigPropertiesProcessor resultProcessor = new ConfigPropertiesProcessor(destinationDir, newClusterName);
      ConfigConverter converter = new ConfigConverter(resultProcessor::process, force);
      converter.processInput(newClusterName, tcConfigFiles.toArray(new Path[0]));
      logger.info("Configuration properties file saved under: {}", destinationDir.toAbsolutePath().normalize());

    } else {
      throw new AssertionError("Unexpected conversion format: " + conversionFormat);
    }

    logger.info("Command successful!" + lineSeparator());
  }
}
