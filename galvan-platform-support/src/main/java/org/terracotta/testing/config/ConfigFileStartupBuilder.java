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
import org.terracotta.ipceventbus.proc.AnyProcess;
import org.terracotta.ipceventbus.proc.AnyProcessBuilder;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ConfigFileStartupBuilder extends StartupCommandBuilder {
  private String[] builtCommand;

  @Override
  public String[] build() {
    if (builtCommand == null) {
      try {
        installServer();
        Path configFile = convertToConfigFile().resolve("cluster.properties");
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

    if (isConsistentStartup()) {
      command.add("-c");
    }

    command.add("-f");
    command.add(configFile.toString());

    command.add("-s");
    command.add("localhost");

    command.add("-p");
    command.add(String.valueOf(getPort()));

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
  private Path convertToConfigFile() {
    Path generatedConfigFileDir = getServerWorkingDir().getParent().getParent().resolve("generated-configs");

    if (Files.exists(generatedConfigFileDir)) {
      // this builder is called for each server, but the CLI will generate the config directories for all.
      return generatedConfigFileDir;
    }

    List<String> command = new ArrayList<>();
    String scriptPath = getAbsolutePath(Paths.get("tools", "upgrade", "bin", "config-converter"));
    command.add(scriptPath);
    command.add("convert");

    for (Path tcConfig : getTcConfigs()) {
      command.add("-c");
      command.add(tcConfig.toString());
    }

    command.add("-t");
    command.add("properties");

    command.add("-d");
    command.add(getServerWorkingDir().relativize(generatedConfigFileDir).toString());

    command.add("-f"); //Do not fail for relative paths

    executeCommand(command, getServerWorkingDir());
    return generatedConfigFileDir;
  }

  private void executeCommand(List<String> command, Path workingDir) {
    AnyProcess process;
    try {
      AnyProcessBuilder<? extends AnyProcess> builder = AnyProcess.newBuilder()
          .command(command.toArray(new String[0]))
          .workingDir(workingDir.toFile())
          .pipeStdout(System.out)
          .pipeStderr(System.err);

      if (getDebugPort() > 0) {
        builder.env("JAVA_OPTS", "-Xdebug -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=" + getDebugPort());
      }
      process = builder.build();
    } catch (Exception e) {
      throw new IllegalStateException("Error launching command: " + String.join(" ", command), e);
    }

    int exitStatus;
    try {
      exitStatus = process.waitFor();
    } catch (InterruptedException e) {
      throw new IllegalStateException(e);
    }

    if (exitStatus != 0) {
      throw new IllegalStateException("Process: '" + String.join(" ", command) + "' executed from '" + workingDir + "' exited with status: " + exitStatus);
    }
  }
}
