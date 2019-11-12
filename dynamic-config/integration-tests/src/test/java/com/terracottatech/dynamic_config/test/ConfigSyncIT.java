/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.terracottatech.dynamic_config.cli.ConfigTool;
import com.terracottatech.dynamic_config.test.util.AppendLogCapturer;
import com.terracottatech.dynamic_config.test.util.NodeProcess;
import org.junit.Ignore;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.junit.Assert.assertThat;

public class ConfigSyncIT extends BaseStartupIT {
  public ConfigSyncIT() {
    super(2, 1);
  }

  @Test
  @Ignore
  public void testPassiveSyncingAppendChangesFromActive() throws Exception {
    int firstNodeId = 1;
    int stripeId = 1;
    int secondNodeId = 2;
    NodeProcess nodeProcess = startNode(
        "--node-name", "node-" + firstNodeId,
        "--node-hostname", "localhost",
        "--node-port", String.valueOf(ports.getPorts()[0]),
        "--node-group-port", String.valueOf(ports.getPorts()[1]),
        "--node-log-dir", "logs/stripe" + stripeId + "/node-" + firstNodeId,
        "--node-metadata-dir", "metadata/stripe" + stripeId,
        "--node-repository-dir", "repository/stripe" + stripeId + "/node-" + firstNodeId,
        "--data-dirs", "main:user-data/main/stripe" + stripeId);

    NodeProcess secondNodeProcess = startNode(
        "--node-name", "node-" + secondNodeId,
        "--node-hostname", "localhost",
        "--node-port", String.valueOf(ports.getPorts()[2]),
        "--node-group-port", String.valueOf(ports.getPorts()[3]),
        "--node-log-dir", "logs/stripe" + stripeId + "/node-" + secondNodeId,
        "--node-metadata-dir", "metadata/stripe" + stripeId,
        "--node-repository-dir", "repository/stripe" + stripeId + "/node-" + secondNodeId,
        "--data-dirs", "main:user-data/main/stripe" + stripeId);

    waitedAssert(out::getLog, stringContainsInOrder(
        Arrays.asList("Started the server in diagnostic mode", "Started the server in diagnostic mode")
    ));

    ConfigTool.main("attach", "-d", "localhost:" + ports.getPorts()[0], "-s", "localhost:" + ports.getPorts()[2]);
    waitedAssert(out::getLog, containsString("Command successful"));
    out.clearLog();
    ConfigTool.main("activate", "-s", "localhost:" + ports.getPorts()[0], "-n", "tc-cluster", "-l", licensePath().toString());
    waitedAssert(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));
    waitedAssert(out::getLog, containsString("Moved to State[ PASSIVE-STANDBY ]"));

    System.out.println("After activation " + secondNodeProcess.getServerState().toString());
    secondNodeProcess.close();
    ConfigTool.main("set", "-s", "localhost:" + ports.getPorts()[0], "-c", "offheap-resources.main=1GB");
    waitedAssert(out::getLog, containsString("Command successful"));
    out.clearLog();

    Path activePath = Paths.get(getBaseDir() + "/repository/stripe" + stripeId + "/node-" + firstNodeId + "/sanskrit");
    Path passivePath = Paths.get(getBaseDir() + "/repository/stripe" + stripeId + "/node-" + secondNodeId + "/sanskrit");
    List<JsonNode> activeChanges = AppendLogCapturer.getChanges(activePath);
    List<JsonNode> passiveChanges = AppendLogCapturer.getChanges(passivePath);
    assertContentsBeforeSync(activeChanges, passiveChanges);
    startNode("-r", "repository/stripe" + stripeId + "/node-" + secondNodeId);

    waitedAssert(out::getLog, containsString("Moved to State[ PASSIVE-STANDBY ]"));
    passiveChanges = AppendLogCapturer.getChanges(passivePath);
    assertContentsAfterSync(activeChanges, passiveChanges);
  }

  private void assertContentsAfterSync(List<JsonNode> activeChanges, List<JsonNode> passiveChanges) {
    assertThat(activeChanges.size(), is(5));
    assertThat(passiveChanges.size(), is(5));

    for (int i = 0; i < passiveChanges.size(); ++i) {
      assertContents(activeChanges, passiveChanges, passiveChanges.size());
    }
  }

  private void assertContentsBeforeSync(List<JsonNode> activeChanges, List<JsonNode> passiveChanges) {
    assertThat(activeChanges.size(), is(5));
    assertThat(passiveChanges.size(), is(3));
    for (int i = 0; i < passiveChanges.size(); ++i) {
      assertContents(activeChanges, passiveChanges, passiveChanges.size());
    }
  }

  private void assertContents(List<JsonNode> activeChanges, List<JsonNode> passiveChanges, int till) {
    for (int i = 0; i < till; ++i) {
      JsonNode active = activeChanges.get(i);
      JsonNode passive = passiveChanges.get(i);
      assertThat(active.get("mutativeMessageCount"), is(passive.get("mutativeMessageCount")));
      assertThat(active.get("mode"), is(passive.get("mode")));
      assertThat(active.get("latestChangeUuid"), is(passive.get("latestChangeUuid")));
      if (active.get("latestChangeUuid") != null) {
        JsonNode activeUuid = active.get("latestChangeUuid");
        JsonNode passiveUuid = passive.get("latestChangeUuid");
        assertThat(activeUuid.get("state"), is(passiveUuid.get("state")));
        assertThat(activeUuid.get("version"), is(passiveUuid.get("version")));
        assertThat(activeUuid.get("prevChangeUuid"), is(passiveUuid.get("prevChangeUuid")));
        assertThat(activeUuid.get("operation"), is(passiveUuid.get("operation")));
      }
    }
  }

}
