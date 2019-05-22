/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config;

import org.junit.Ignore;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.fail;
import static org.hamcrest.Matchers.containsString;

public class NewServerStartupScriptIT extends BaseStartupIT {
  @Test
  @Ignore("might cause build failures as configuration repo uses a pre-defined port")
  public void testStartingWithSingleStripeSingleNodeRepo() throws Exception {
    String stripeName = "stripe1";
    String nodeName = "server-1";
    Path configurationRepo = configRepoPath(singleStripeSingleNodeNomadRoot(stripeName, nodeName), nodeName);
    startServer(getScriptPath(), "--node-config-dir", configurationRepo.toString());
    waitedAssert(systemOutRule::getLog, containsString("Becoming State[ ACTIVE-COORDINATOR ]"));
  }

  @Test
  @Ignore("might cause build failures as configuration repo uses a pre-defined port")
  public void testStartingWithSingleStripeMultiNodeRepo() throws Exception {
    String stripeName = "stripe1";
    String nodeName = "server-2";
    Path configurationRepo = configRepoPath(singleStripeMultiNodeNomadRoot(stripeName, nodeName), nodeName);
    startServer(getScriptPath(), "--node-config-dir", configurationRepo.toString());
    waitedAssert(systemOutRule::getLog, containsString("Becoming State[ ACTIVE-COORDINATOR ]"));
  }

  @Test
  @Ignore("might cause build failures as configuration repo uses a pre-defined port")
  public void testStartingWithMultiStripeRepo() throws Exception {
    String stripeName = "stripe2";
    String nodeName = "server-1";
    Path configurationRepo = configRepoPath(multiStripeNomadRoot(stripeName, nodeName), nodeName);
    startServer(getScriptPath(), "--node-config-dir", configurationRepo.toString());
    waitedAssert(systemOutRule::getLog, containsString("Becoming State[ ACTIVE-COORDINATOR ]"));
  }

  @Test
  public void testStartingWithNonExistentRepo() throws Exception {
    String configurationRepo = temporaryFolder.newFolder().getAbsolutePath();
    startServer(getScriptPath(), "-c", configurationRepo);
    waitedAssert(systemOutRule::getLog, containsString("Started the server in diagnostic mode"));
  }

  @Test
  public void testStartingWithSingleNodeConfigFile() throws Exception {
    Path configurationFile = configFilePath();
    startServer(getScriptPath(), "--config-file", configurationFile.toString());
    waitedAssert(systemOutRule::getLog, containsString("Started the server in diagnostic mode"));
  }

  @Test
  public void testStartingWithSingleNodeConfigFileWithHostPort() throws Exception {
    String port = String.valueOf(portChooser.choosePorts(1).getPort());
    Path configurationFile = configFilePath("", port);
    startServer(getScriptPath(), "-f", configurationFile.toString(), "-s", "localhost", "-p", port);
    waitedAssert(systemOutRule::getLog, containsString("Started the server in diagnostic mode"));
  }

  @Test
  public void testFailedStartupConfigFile_nonExistentFile() throws Exception {
    Path configurationFile = Paths.get(".").resolve("blah");
    startServer(getScriptPath(), "--config-file", configurationFile.toString());
    waitedAssert(systemOutRule::getLog, containsString("FileNotFoundException"));
  }

  @Test
  public void testFailedStartupConfigFile_invalidPort() throws Exception {
    String port = String.valueOf(portChooser.choosePorts(1).getPort());
    Path configurationFile = configFilePath("_invalid1", port);
    startServer(getScriptPath(), "--config-file", configurationFile.toString(), "--node-hostname", "localhost", "--node-port", port);
    waitedAssert(systemOutRule::getLog, containsString("<port> specified in node-port=<port> must be an integer between 1 and 65535"));
  }

  @Test
  public void testFailedStartupConfigFile_invalidSecurity() throws Exception {
    String port = String.valueOf(portChooser.choosePorts(1).getPort());
    Path configurationFile = configFilePath("_invalid2", port);
    startServer(getScriptPath(), "--config-file", configurationFile.toString(), "--node-hostname", "localhost", "--node-port", port);
    waitedAssert(systemOutRule::getLog, containsString("security-dir is mandatory for any of the security configuration"));
  }

  @Test
  public void testFailedStartupConfigFile_invalidCliParams() throws Exception {
    Path configurationFile = configFilePath();
    startServer(getScriptPath(), "--config-file", configurationFile.toString(), "--node-bind-address", "::1");
    waitedAssert(systemOutRule::getLog, containsString("'--config-file' parameter can only be used with '--node-hostname', '--node-port', and '--node-config-dir' parameters"));
  }

  @Test
  public void testFailedStartupCliParams_invalidAuthc() throws Exception {
    startServer(getScriptPath(), "--security-authc=blah");
    waitedAssert(systemOutRule::getLog, containsString("security-authc should be one of: [file, ldap, certificate]"));
  }

  @Test
  public void testFailedStartupCliParams_invalidHostname() throws Exception {
    startServer(getScriptPath(), "--node-hostname=:::");
    waitedAssert(systemOutRule::getLog, containsString("<address> specified in node-hostname=<address> must be a valid hostname or IP address"));
  }

  @Test
  public void testFailedStartupCliParams_invalidFailoverPriority() throws Exception {
    startServer(getScriptPath(), "--failover-priority=blah");
    waitedAssert(systemOutRule::getLog, containsString("failover-priority should be one of: [availability, consistency]"));
  }

  @Test
  public void testFailedStartupCliParams_invalidSecurity() throws Exception {
    startServer(getScriptPath(), "--security-audit-log-dir", "audit-dir");
    waitedAssert(systemOutRule::getLog, containsString("security-dir is mandatory for any of the security configuration"));
  }

  @Test
  public void testSuccessfulStartupCliParams() throws Exception {
    startServer(getScriptPath(), "-p", String.valueOf(portChooser.choosePorts(1).getPort()));
    waitedAssert(systemOutRule::getLog, containsString("Started the server in diagnostic mode"));
  }

  private Path getScriptPath() {
    String kitInstallationPath = System.getProperty("kitInstallationPath");
    if (kitInstallationPath == null) {
      fail("Terracotta kit install location is not configured");
    }

    return Paths.get(kitInstallationPath)
        .resolve("server")
        .resolve("bin")
        .resolve("start-node." + (isWindows() ? "bat" : "sh"));
  }

  private Path configFilePath() throws Exception {
    return configFilePath("", String.valueOf(portChooser.choosePorts(1).getPort()));
  }

  private Path configFilePath(String suffix, String port) throws Exception {
    String resourceName = "/config-property-files/single-stripe" + suffix + ".properties";
    Path original = Paths.get(NewServerStartupScriptIT.class.getResource(resourceName).toURI());
    String contents = new String(Files.readAllBytes(original));
    String replacedContents = contents.replaceAll(Pattern.quote("${PORT}"), port);
    Path newPath = temporaryFolder.newFile().toPath();
    Files.write(newPath, replacedContents.getBytes(StandardCharsets.UTF_8));
    return newPath;
  }
}