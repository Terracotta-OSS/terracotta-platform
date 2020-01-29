/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.system_tests;

import org.terracotta.dynamic_config.system_tests.util.ConfigRepositoryGenerator;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static java.lang.System.lineSeparator;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.write;
import static org.hamcrest.Matchers.containsString;

public class SimpleOldServerStartupScriptIT extends BaseStartupIT {
  @Test
  public void testStartingWithSingleStripeSingleNodeRepo() throws Exception {
    String configurationRepo = generateNodeRepositoryDir(1, 1, ConfigRepositoryGenerator::generate1Stripe1Node).toString();
    startTcServer("-r", configurationRepo, "-n", "node-1", "--node-name", "node-1");
    waitedAssert(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));
  }

  @Test
  public void testStartingWithConsistencyMode() throws Exception {
    startTcServer("--config", createTcConfig().toString(), "--config-consistency", "-r", getNodeRepositoryDir().toString(), "--node-name", "node-1");
    waitedAssert(out::getLog, containsString("Started the server in diagnostic mode"));
  }

  @Test
  public void testStartingWithTcConfig() throws Exception {
    startTcServer("-f", createTcConfig().toString());
    waitedAssert(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));
  }

  private Path createTcConfig() throws Exception {
    Path tcConfigPath = getBaseDir().resolve("server-config.xml").toAbsolutePath();
    String serverConfiguration = "<tc-config xmlns=\"http://www.terracotta.org/config\" xmlns:data=\"http://www.terracottatech.com/config/data-roots\">" + lineSeparator() +
        lineSeparator() +
        "  <plugins>" + lineSeparator() +
        "     <config>" + lineSeparator() +
        "       <data:data-directories>" + lineSeparator() +
        "         <data:directory name=\"data\" " +
        "use-for-platform=\"true\">%(user.dir)/metadata/stripe1</data:directory>" + lineSeparator() +
        "       </data:data-directories>" + lineSeparator() +
        "     </config>" + lineSeparator() +
        "  </plugins>" + lineSeparator() +
        "  <servers>" + lineSeparator() +
        "    <server host=\"localhost\" name=\"node-1\">" + lineSeparator() +
        "      <logs>%(user.dir)/logs/stripe1/node-1</logs>" + lineSeparator() +
        "      <tsa-port>${TSA_PORT}</tsa-port>" + lineSeparator() +
        "      <tsa-group-port>${GROUP_PORT}</tsa-group-port>" + lineSeparator() +
        "    </server>" + lineSeparator() +
        "  </servers>" + lineSeparator() +
        "</tc-config>";

    int[] ports = this.ports.getPorts();
    serverConfiguration = serverConfiguration
        .replace("${TSA_PORT}", String.valueOf(ports[0]))
        .replace("${GROUP_PORT}", String.valueOf(ports[1]));

    Files.createDirectories(getBaseDir());
    write(tcConfigPath, serverConfiguration.getBytes(UTF_8));
    return tcConfigPath;
  }
}