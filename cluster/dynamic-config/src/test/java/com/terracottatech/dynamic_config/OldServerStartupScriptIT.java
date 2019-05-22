/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config;

import org.junit.Ignore;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.write;
import static org.assertj.core.api.Assertions.fail;
import static org.hamcrest.Matchers.containsString;

public class OldServerStartupScriptIT extends BaseStartupIT {
  @Test
  @Ignore("might cause build failures as configuration repo uses a pre-defined port")
  public void testStartingWithSingleStripeSingleNodeRepo() throws Exception {
    String stripeName = "stripe1";
    String nodeName = "server-1";
    Path configurationRepo = configRepoPath(singleStripeSingleNodeNomadRoot(stripeName, nodeName), nodeName);
    startServer(getScriptPath(), "-r", configurationRepo.toString());
    waitedAssert(systemOutRule::getLog, containsString("Becoming State[ ACTIVE-COORDINATOR ]"));
  }

  @Test
  @Ignore("might cause build failures as configuration repo uses a pre-defined port")
  public void testStartingWithSingleStripeMultiNodeRepo() throws Exception {
    String stripeName = "stripe1";
    String nodeName = "server-2";
    Path configurationRepo = configRepoPath(singleStripeMultiNodeNomadRoot(stripeName, nodeName), nodeName);
    startServer(getScriptPath(), "-r", configurationRepo.toString(), "-n", nodeName);
    waitedAssert(systemOutRule::getLog, containsString("Becoming State[ ACTIVE-COORDINATOR ]"));
  }

  @Test
  @Ignore("might cause build failures as configuration repo uses a pre-defined port")
  public void testStartingWithMultiStripeRepo() throws Exception {
    String stripeName = "stripe2";
    String nodeName = "server-3";
    Path configurationRepo = configRepoPath(multiStripeNomadRoot(stripeName, nodeName), nodeName);
    startServer(getScriptPath(), "-r", configurationRepo.toString(), "-n", nodeName);
    waitedAssert(systemOutRule::getLog, containsString("Becoming State[ ACTIVE-COORDINATOR ]"));
  }

  @Test
  public void testStartingWithConsistencyMode() throws Exception {
    startServer(getScriptPath(), "--config", getConfigurationPath(), "--config-consistency");
    waitedAssert(systemOutRule::getLog, containsString("Started the server in diagnostic mode"));
  }

  @Test
  public void testStartingWithEmptyConfigurationRepo() throws Exception {
    String configurationRepo = temporaryFolder.newFolder().getAbsolutePath();
    startServer(getScriptPath(), "-r", configurationRepo);
    waitedAssert(systemOutRule::getLog, containsString("restart the server in 'config-consistency' mode"));
  }

  @Test
  public void testStartingWithTcConfig() throws Exception {
    startServer(getScriptPath(), "-f", getConfigurationPath());
    waitedAssert(systemOutRule::getLog, containsString("Becoming State[ ACTIVE-COORDINATOR ]"));
  }

  private String getConfigurationPath() throws Exception {
    int serverPort = portChooser.choosePorts(2).getPort();

    Path serverConfigurationPath = temporaryFolder.newFolder().toPath().resolve("server-config.xml").toAbsolutePath();
    String dataRootLocation = temporaryFolder.newFolder().getAbsolutePath();

    String serverConfiguration = "<tc-config xmlns=\"http://www.terracotta.org/config\" xmlns:data=\"http://www.terracottatech.com/config/data-roots\">\n" +
        "\n" +
        "  <plugins>\n" +
        "     <config>\n" +
        "       <data:data-directories>\n" +
        "         <data:directory name=\"data\" " +
        "use-for-platform=\"true\">${DATA_ROOT}</data:directory>\n" +
        "       </data:data-directories>\n" +
        "     </config>\n" +
        "  </plugins>\n" +
        "  <servers>\n" +
        "    <server host=\"localhost\" name=\"server-1\">\n" +
        "      <tsa-port>${TSA_PORT}</tsa-port>\n" +
        "      <tsa-group-port>${GROUP_PORT}</tsa-group-port>\n" +
        "    </server>\n" +
        "  </servers>\n" +
        "</tc-config>";

    serverConfiguration = serverConfiguration.replaceAll(Pattern.quote("${TSA_PORT}"), String.valueOf(serverPort))
        .replaceAll(Pattern.quote("${GROUP_PORT}"), String.valueOf(serverPort + 1))
        .replaceAll(Pattern.quote("${DATA_ROOT}"), dataRootLocation);

    write(serverConfigurationPath, serverConfiguration.getBytes(UTF_8));
    return serverConfigurationPath.toAbsolutePath().toString();
  }

  private Path getScriptPath() {
    String kitInstallationPath = System.getProperty("kitInstallationPath");
    if (kitInstallationPath == null) {
      fail("Terracotta kit install location is not configured");
    }

    return Paths.get(kitInstallationPath)
        .resolve("server")
        .resolve("bin")
        .resolve("start-tc-server." + (isWindows() ? "bat" : "sh"));
  }
}