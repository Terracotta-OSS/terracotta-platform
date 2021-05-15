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

import com.beust.jcommander.ParameterException;
import org.terracotta.dynamic_config.cli.api.command.Injector.Inject;
import org.terracotta.dynamic_config.cli.api.output.OutputService;
import org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.ConfigConverter;
import org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.ConfigPropertiesProcessor;
import org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.ConfigRepoProcessor;
import org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.ConversionFormat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static java.nio.file.Files.isDirectory;
import static org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.ConversionFormat.DIRECTORY;
import static org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.ConversionFormat.PROPERTIES;

public class ConvertAction implements Runnable {

  private List<Path> tcConfigFiles;
  private List<String> stripeNames;
  private Path licensePath;
  private Path destinationDir = Paths.get(".").resolve("converted-configs");
  private String newClusterName;
  private ConversionFormat conversionFormat = DIRECTORY;
  private boolean force;

  @Inject public OutputService output;

  public void setTcConfigFiles(List<Path> tcConfigFiles) {
    this.tcConfigFiles = tcConfigFiles;
  }

  public void setStripeNames(List<String> stripeNames) {
    this.stripeNames = stripeNames;
  }

  public void setLicensePath(Path licensePath) {
    this.licensePath = licensePath;
  }

  public void setDestinationDir(Path destinationDir) {
    this.destinationDir = destinationDir;
  }

  public void setNewClusterName(String newClusterName) {
    this.newClusterName = newClusterName;
  }

  public void setConversionFormat(ConversionFormat conversionFormat) {
    this.conversionFormat = conversionFormat;
  }

  public void setForce(boolean force) {
    this.force = force;
  }

  public void validate() {
    if (stripeNames == null) {
      stripeNames = Collections.emptyList();
    }

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
    validate();
    if (conversionFormat == DIRECTORY) {
      ConfigRepoProcessor resultProcessor = new ConfigRepoProcessor(destinationDir);
      ConfigConverter converter = new ConfigConverter(resultProcessor::process, force);
      converter.processInput(newClusterName, stripeNames, tcConfigFiles.toArray(new Path[0]));

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
      output.info("Configuration directories saved under: {}", destinationDir.toAbsolutePath().normalize());

    } else if (conversionFormat == PROPERTIES) {
      ConfigPropertiesProcessor resultProcessor = new ConfigPropertiesProcessor(destinationDir, newClusterName);
      ConfigConverter converter = new ConfigConverter(resultProcessor::process, force);
      converter.processInput(newClusterName, stripeNames, tcConfigFiles.toArray(new Path[0]));
      output.info("Configuration properties file saved under: {}", destinationDir.toAbsolutePath().normalize());

    } else {
      throw new AssertionError("Unexpected conversion format: " + conversionFormat);
    }

    output.info("Command successful!");
  }
}
