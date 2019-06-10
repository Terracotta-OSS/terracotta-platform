/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.test;

import com.terracottatech.dynamic_config.test.util.Kit;
import com.terracottatech.dynamic_config.test.util.NodeProcess;
import org.junit.Test;

import java.nio.file.Path;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.write;
import static org.hamcrest.Matchers.containsString;

public class OldServerStartupScriptIT extends BaseStartupIT {
  @Test
  public void testStartingWithSingleStripeSingleNodeRepo() throws Exception {
    String stripeName = "stripe1";
    String nodeName = "testServer1";
    Path configurationRepo = configRepoPath(singleStripeSingleNodeNomadRoot(stripeName, nodeName));
    startServer("-r", configurationRepo.toString());
    waitedAssert(out::getLog, containsString("Becoming State[ ACTIVE-COORDINATOR ]"));
  }

  @Test
  public void testStartingWithSingleStripeMultiNodeRepo() throws Exception {
    String stripeName = "stripe1";
    String nodeName = "testServer2";
    Path configurationRepo = configRepoPath(singleStripeMultiNodeNomadRoot(stripeName, nodeName));
    startServer("-r", configurationRepo.toString(), "-n", nodeName);
    waitedAssert(out::getLog, containsString("Becoming State[ ACTIVE-COORDINATOR ]"));
  }

  @Test
  public void testStartingWithMultiStripeRepo() throws Exception {
    String stripeName = "stripe2";
    String nodeName = "testServer2";
    Path configurationRepo = configRepoPath(multiStripeNomadRoot(stripeName, nodeName));
    startServer("-r", configurationRepo.toString(), "-n", nodeName);
    waitedAssert(out::getLog, containsString("Becoming State[ ACTIVE-COORDINATOR ]"));
  }

  @Test
  public void testStartingWithConsistencyMode() throws Exception {
    startServer("--config", getConfigurationPath(), "--config-consistency");
    waitedAssert(out::getLog, containsString("Started the server in diagnostic mode"));
  }

  @Test
  public void testStartingWithEmptyConfigurationRepo() throws Exception {
    String configurationRepo = temporaryFolder.newFolder().getAbsolutePath();
    startServer("-r", configurationRepo);
    waitedAssert(out::getLog, containsString("No configuration files found"));
  }

  @Test
  public void testStartingWithTcConfig() throws Exception {
    startServer("-f", getConfigurationPath());
    waitedAssert(out::getLog, containsString("Becoming State[ ACTIVE-COORDINATOR ]"));
  }

  private String getConfigurationPath() throws Exception {
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
        "    <server host=\"localhost\" name=\"testServer1\">\n" +
        "      <tsa-port>${TSA_PORT}</tsa-port>\n" +
        "      <tsa-group-port>${GROUP_PORT}</tsa-group-port>\n" +
        "    </server>\n" +
        "  </servers>\n" +
        "</tc-config>";

    int[] ports = this.ports.getPorts();
    serverConfiguration = serverConfiguration.replaceAll(Pattern.quote("${TSA_PORT}"), String.valueOf(ports[0]))
        .replaceAll(Pattern.quote("${GROUP_PORT}"), String.valueOf(ports[1]))
        .replaceAll(Pattern.quote("${DATA_ROOT}"), dataRootLocation);

    write(serverConfigurationPath, serverConfiguration.getBytes(UTF_8));
    return serverConfigurationPath.toAbsolutePath().toString();
  }

  private void startServer(String... cli) {
    nodeProcess = NodeProcess.startTcServer(Kit.getOrCreatePath(), cli);
  }

}