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
package org.terracotta.testing.config;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.ConfigConverterTool;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ConfigFileStartupBuilder extends AbstractStartupCommandBuilder {
  private String[] builtCommand;

  public ConfigFileStartupBuilder() {
  }

  @Override
  public String[] build() {
    if (builtCommand == null) {
      try {
        installServer();
        Path configFile = convertToConfigFile(true).resolve("test.properties");
        configFileStartupCommand(configFile);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
    return builtCommand.clone();
  }

  private void configFileStartupCommand(Path configFile) {
    List<String> command = new ArrayList<>();
    String scriptPath = getAbsolutePath(Paths.get("server", "bin", "start-tc-server"));
    command.add(scriptPath);

    command.add("-f");
    command.add(configFile.toString());

    command.add("-n");
    command.add(getServerName());

    command.add("--auto-activate");

    Path configDir;
    try {
      configDir = Files.createTempDirectory(getServerWorkingDir(), "config");
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    command.add("-r");
    command.add(configDir.toString());

    builtCommand = command.toArray(new String[0]);
  }

  @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
  protected Path convertToConfigFile(boolean properties) {
    Path generatedConfigFileDir = getServerWorkingDir().getParent().resolve("generated-configs");

    if (Files.exists(generatedConfigFileDir)) {
      // this builder is called for each server, but the CLI will generate the config directories for all.
      return generatedConfigFileDir;
    }

    List<String> command = new ArrayList<>();
    command.add("convert");

    command.add("-c");
    command.add(getServerWorkingDir().resolve(getTcConfig()).toString());

    for (int i = 0; i < 1; i++) {
      command.add("-s");
      command.add("stripe[" + i + "]");
    }
    if (properties) {
      command.add("-t");
      command.add("properties");
    }

    command.add("-d");
    command.add(generatedConfigFileDir.toString());

    command.add("-f"); //Do not fail for relative paths

    command.add("-n");
    command.add(getClusterName());

    executeCommand(command);
    return generatedConfigFileDir;
  }

  protected static void executeCommand(List<String> command) {
    new ConfigConverterTool().run(command.toArray(new String[0]));
  }
}
