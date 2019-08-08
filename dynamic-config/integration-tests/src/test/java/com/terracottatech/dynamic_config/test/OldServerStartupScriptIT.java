/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.test;

import com.terracottatech.dynamic_config.test.util.Kit;
import com.terracottatech.dynamic_config.test.util.NodeProcess;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.write;
import static org.hamcrest.Matchers.containsString;

public class OldServerStartupScriptIT extends BaseStartupIT {
  @Test
  public void testStartingWithSingleStripeSingleNodeRepo() throws Exception {
    String configurationRepo = copyServerConfigFiles(1, 1, this::singleStripeSingleNode).toString();
    startServer("-r", configurationRepo, "-n", "node-1", "--node-name", "node-1");
    waitedAssert(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));
  }

  @Test
  public void testStartingWithSingleStripeMultiNodeRepo() throws Exception {
    String configurationRepo = copyServerConfigFiles(1, 2, this::singleStripeMultiNode).toString();
    startServer("-r", configurationRepo, "-n", "node-2", "--node-name", "node-2");
    waitedAssert(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));
  }

  @Test
  public void testStartingWithMultiStripeRepo() throws Exception {
    String configurationRepo = copyServerConfigFiles(2, 1, this::multiStripe).toString();
    startServer("-r", configurationRepo, "-n", "node-1", "--node-name", "node-1");
    waitedAssert(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));
  }

  @Test
  public void testStartingWithConsistencyMode() throws Exception {
    startServer("--config", createTcConfig().toString(), "--config-consistency", "-r", configRepositoryPath().toString(), "--node-name", "node-1");
    waitedAssert(out::getLog, containsString("Started the server in diagnostic mode"));
  }

  @Test
  public void testStartingWithTcConfig() throws Exception {
    startServer("-f", createTcConfig().toString());
    waitedAssert(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));
  }

  private Path createTcConfig() throws Exception {
    Path tcConfigPath = getBaseDir().resolve("server-config.xml").toAbsolutePath();
    String serverConfiguration = "<tc-config xmlns=\"http://www.terracotta.org/config\" xmlns:data=\"http://www.terracottatech.com/config/data-roots\">\n" +
        "\n" +
        "  <plugins>\n" +
        "     <config>\n" +
        "       <data:data-directories>\n" +
        "         <data:directory name=\"data\" " +
        "use-for-platform=\"true\">%(user.dir)/metadata/stripe1</data:directory>\n" +
        "       </data:data-directories>\n" +
        "     </config>\n" +
        "  </plugins>\n" +
        "  <servers>\n" +
        "    <server host=\"localhost\" name=\"node-1\">\n" +
        "      <logs>%(user.dir)/logs/stripe1/node-1</logs>\n" +
        "      <tsa-port>${TSA_PORT}</tsa-port>\n" +
        "      <tsa-group-port>${GROUP_PORT}</tsa-group-port>\n" +
        "    </server>\n" +
        "  </servers>\n" +
        "</tc-config>";

    int[] ports = this.ports.getPorts();
    serverConfiguration = serverConfiguration
        .replace("${TSA_PORT}", String.valueOf(ports[0]))
        .replace("${GROUP_PORT}", String.valueOf(ports[1]));

    Files.createDirectories(getBaseDir());
    write(tcConfigPath, serverConfiguration.getBytes(UTF_8));
    return tcConfigPath;
  }

  private void startServer(String... cli) {
    nodeProcesses.add(NodeProcess.startTcServer(Kit.getOrCreatePath(), getBaseDir(), cli));
  }

}