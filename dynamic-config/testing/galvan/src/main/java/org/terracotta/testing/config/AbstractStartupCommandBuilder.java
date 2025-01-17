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
package org.terracotta.testing.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.terracotta.testing.demos.TestHelpers.isWindows;

public abstract class AbstractStartupCommandBuilder implements StartupCommandBuilder {
  private Path kitDir;
  private Path serverWorkingDir;
  private String logConfigExt;
  private String serverName;
  private boolean consistentStartup;
  private Path tcConfig;
  private final int debugPort = Integer.getInteger("configDebugPort", 0);
  private int port;
  private int stripeId;
  private String clusterName = ConfigConstants.DEFAULT_CLUSTER_NAME;
  private Path licensePath;

  public StartupCommandBuilder port(int port) {
    this.port = port;
    return this;
  }

  public StartupCommandBuilder serverWorkingDir(Path serverWorkingDir) {
    this.serverWorkingDir = serverWorkingDir;
    return this;
  }

  public StartupCommandBuilder serverName(String serverName) {
    this.serverName = serverName;
    return this;
  }

  public StartupCommandBuilder consistentStartup(boolean consistentStartup) {
    this.consistentStartup = consistentStartup;
    return this;
  }

  public StartupCommandBuilder kitDir(Path kitDir) {
    this.kitDir = kitDir;
    return this;
  }

  public StartupCommandBuilder logConfigExtension(String logConfigExt) {
    this.logConfigExt = logConfigExt;
    return this;
  }

  public StartupCommandBuilder tcConfig(Path tcConfig) {
    this.tcConfig = tcConfig;
    return this;
  }

  public StartupCommandBuilder clusterName(String clusterName) {
    this.clusterName = clusterName;
    return this;
  }

  public StartupCommandBuilder license(Path licensePath) {
    this.licensePath = licensePath;
    return this;
  }

  public StartupCommandBuilder stripeName(String stripeName) {
    this.stripeId = Integer.parseInt(stripeName.substring(6));
    return this;
  }

  protected void installServer() throws IOException {
    // Create a copy of the server for this installation.
    Files.createDirectories(serverWorkingDir);

    //TODO: use the new Files utility
    //Copy a custom logback configuration

    Files.copy(this.getClass().getResourceAsStream("/tc-logback.xml"), serverWorkingDir.resolve("logback-test.xml"), REPLACE_EXISTING);
    Properties props = new Properties();
    props.setProperty("serverWorkingDir", serverWorkingDir.toAbsolutePath().toString());
    try (Writer w = new OutputStreamWriter(Files.newOutputStream(serverWorkingDir.resolve("logbackVars.properties").toFile().toPath()), StandardCharsets.UTF_8)) {
      props.store(w, "logging variables");
    }
    if (logConfigExt != null) {
      InputStream logExt = this.getClass().getResourceAsStream("/" + logConfigExt);
      if (logExt != null) {
        //TODO: use the new Files utility
        Files.copy(logExt, serverWorkingDir.resolve("logback-ext-test.xml"), REPLACE_EXISTING);
      }
    }
  }

  /**
   * Returns a normalized absolute path to the shell/bat script, and quotes the windows path to avoid issues with special path chars.
   * @param scriptPath path to the script from the base kit
   * @return string representation of processed path
   */
  protected String getAbsolutePath(Path scriptPath) {
    Path basePath =  serverWorkingDir.resolve(kitDir).resolve(scriptPath).toAbsolutePath().normalize();
    return isWindows() ? "\"" + basePath + ".bat\"" : basePath + ".sh";
  }

  public abstract String[] build();

  public Path getKitDir() {
    return kitDir;
  }

  public Path getServerWorkingDir() {
    return serverWorkingDir;
  }

  public String getLogConfigExt() {
    return logConfigExt;
  }

  public String getServerName() {
    return serverName;
  }

  public boolean isConsistentStartup() {
    return consistentStartup;
  }

  public Path getTcConfig() {
    return tcConfig;
  }

  public int getDebugPort() {
    return debugPort;
  }

  public int getPort() {
    return port;
  }

  public int getStripeId() {
    return stripeId;
  }

  public String getClusterName() {
    return clusterName;
  }

  public Path getLicensePath() {
    return licensePath;
  }
}
