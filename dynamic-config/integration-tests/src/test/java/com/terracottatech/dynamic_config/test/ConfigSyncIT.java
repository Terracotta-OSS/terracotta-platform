/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.test;

import com.terracottatech.dynamic_config.cli.ConfigTool;
import com.terracottatech.dynamic_config.test.util.AppendLogCapturer;
import com.terracottatech.dynamic_config.test.util.NodeProcess;
import com.terracottatech.persistence.sanskrit.SanskritObject;
import org.hamcrest.Matcher;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import static com.terracottatech.dynamic_config.nomad.persistence.NomadSanskritKeys.CHANGE_OPERATION;
import static com.terracottatech.dynamic_config.nomad.persistence.NomadSanskritKeys.CHANGE_STATE;
import static com.terracottatech.dynamic_config.nomad.persistence.NomadSanskritKeys.CHANGE_VERSION;
import static com.terracottatech.dynamic_config.nomad.persistence.NomadSanskritKeys.LATEST_CHANGE_UUID;
import static com.terracottatech.dynamic_config.nomad.persistence.NomadSanskritKeys.MODE;
import static com.terracottatech.dynamic_config.nomad.persistence.NomadSanskritKeys.MUTATIVE_MESSAGE_COUNT;
import static com.terracottatech.dynamic_config.nomad.persistence.NomadSanskritKeys.PREV_CHANGE_UUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class ConfigSyncIT extends BaseStartupIT {
  private static final int TIMEOUT = 50;

  public ConfigSyncIT() {
    super(2, 1);
  }

  @Test
  public void testPassiveSyncingAppendChangesFromActive() throws Exception {
    int firstNodeId = 1;
    int stripeId = 1;
    int secondNodeId = 2;
    NodeProcess nodeProcess = startNode(
        "--node-name", "node-" + firstNodeId,
        "--node-hostname", "localhost",
        "--node-port", String.valueOf(ports.getPort()),
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

    waitForAssert(out::getLog, stringContainsInOrder(
        Arrays.asList("Started the server in diagnostic mode", "Started the server in diagnostic mode")
    ));

    ConfigTool.start("attach", "-d", "localhost:" + ports.getPort(), "-s", "localhost:" + ports.getPorts()[2]);
    waitForAssert(out::getLog, containsString("Command successful"));
    out.clearLog();
    ConfigTool.start("activate", "-s", "localhost:" + ports.getPort(), "-n", "tc-cluster", "-l", licensePath().toString());
    waitForAssert(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));
    waitForAssert(out::getLog, containsString("Moved to State[ PASSIVE-STANDBY ]"));

    secondNodeProcess.close();
    ConfigTool.start("set", "-s", "localhost:" + ports.getPort(), "-c", "offheap-resources.main=1GB");
    waitForAssert(out::getLog, containsString("Command successful"));
    out.clearLog();

    Path activePath = Paths.get(getBaseDir() + "/repository/stripe" + stripeId + "/node-" + firstNodeId + "/sanskrit");
    Path passivePath = Paths.get(getBaseDir() + "/repository/stripe" + stripeId + "/node-" + secondNodeId + "/sanskrit");
    List<SanskritObject> activeChanges = AppendLogCapturer.getChanges(activePath);
    List<SanskritObject> passiveChanges = AppendLogCapturer.getChanges(passivePath);
    assertContentsBeforeSync(activeChanges, passiveChanges, 5, 3);
    startNode("-r", "repository/stripe" + stripeId + "/node-" + secondNodeId);

    waitForAssert(out::getLog, containsString("Moved to State[ PASSIVE-STANDBY ]"));
    passiveChanges = AppendLogCapturer.getChanges(passivePath);
    assertContentsAfterSync(activeChanges, passiveChanges, 5, 5);
  }

  @Test
  public void testPassiveZapsActiveHasSomeUnCommittedChanges() throws Exception {
    int firstNodeId = 1;
    int stripeId = 1;
    int secondNodeId = 2;
    NodeProcess nodeProcess = startNode(
        "--node-name", "node-" + firstNodeId,
        "--node-hostname", "localhost",
        "--node-port", String.valueOf(ports.getPort()),
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

    waitForAssert(out::getLog, stringContainsInOrder(
        Arrays.asList("Started the server in diagnostic mode", "Started the server in diagnostic mode")
    ));

    ConfigTool.start("attach", "-d", "localhost:" + ports.getPort(), "-s", "localhost:" + ports.getPorts()[2]);
    waitForAssert(out::getLog, containsString("Command successful"));
    out.clearLog();
    ConfigTool.start("activate", "-s", "localhost:" + ports.getPort(), "-n", "tc-cluster", "-l", licensePath().toString());
    waitForAssert(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));
    waitForAssert(out::getLog, containsString("Moved to State[ PASSIVE-STANDBY ]"));

    secondNodeProcess.close();
    try {
      ConfigTool.start("set", "-s", "localhost:" + ports.getPort(), "-c", "stripe.1.node.1.tc-properties.com.terracottatech.dynamic-config.simulate=commit-failure");
      fail("Expected to throw exception");
    } catch (IllegalStateException e) {
      Path activePath = Paths.get(getBaseDir() + "/repository/stripe" + stripeId + "/node-" + firstNodeId + "/sanskrit");
      Path passivePath = Paths.get(getBaseDir() + "/repository/stripe" + stripeId + "/node-" + secondNodeId + "/sanskrit");
      List<SanskritObject> activeChanges = AppendLogCapturer.getChanges(activePath);
      List<SanskritObject> passiveChanges = AppendLogCapturer.getChanges(passivePath);
      assertContentsBeforeSync(activeChanges, passiveChanges, 4, 3);
      startNode("-r", "repository/stripe" + stripeId + "/node-" + secondNodeId);

      waitForAssert(out::getLog, containsString("Active has some PREPARED changes that is not yet committed."));
      passiveChanges = AppendLogCapturer.getChanges(passivePath);
      assertContentsAfterSync(activeChanges, passiveChanges, 4, 3);
    }
  }

  @Test
  public void testPassiveZapsAppendLogHistoryMismatch() throws Exception {
    int firstNodeId = 1;
    int stripeId = 1;
    int secondNodeId = 2;
    NodeProcess firstNodeProcess = startNode(
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

    waitForAssert(out::getLog, stringContainsInOrder(
        Arrays.asList("Started the server in diagnostic mode", "Started the server in diagnostic mode")
    ));

    ConfigTool.start("attach", "-d", "localhost:" + ports.getPort(), "-s", "localhost:" + ports.getPorts()[2]);
    waitForAssert(out::getLog, containsString("Command successful"));
    out.clearLog();
    ConfigTool.start("activate", "-s", "localhost:" + ports.getPort(), "-n", "tc-cluster", "-l", licensePath().toString());
    waitForAssert(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));
    waitForAssert(out::getLog, containsString("Moved to State[ PASSIVE-STANDBY ]"));

    boolean isFirstNodePassive = false;
    if (firstNodeProcess.getServerState().toString().equals("PASSIVE")) {
      isFirstNodePassive = true;
    }
    if (isFirstNodePassive) {
      try {
        ConfigTool.start("set", "-s", "localhost:" + ports.getPort(), "-c", "stripe.1.node.2.tc-properties.com.terracottatech.dynamic-config.simulate=commit-failure");
        fail("Expected to throw exception");
      } catch (IllegalStateException e) {
        Path passivePath = Paths.get(getBaseDir() + "/repository/stripe" + stripeId + "/node-" + firstNodeId + "/sanskrit");
        Path activePath = Paths.get(getBaseDir() + "/repository/stripe" + stripeId + "/node-" + secondNodeId + "/sanskrit");
        List<SanskritObject> activeChanges = AppendLogCapturer.getChanges(activePath);
        List<SanskritObject> passiveChanges = AppendLogCapturer.getChanges(passivePath);
        assertContentsBeforeSync(activeChanges, passiveChanges, 4, 5);
        firstNodeProcess.close();
        startNode("-r", "repository/stripe" + stripeId + "/node-" + firstNodeId);

        waitForAssert(out::getLog, containsString("Passive cannot sync because the change history does not match"));
        passiveChanges = AppendLogCapturer.getChanges(passivePath);
        assertContentsAfterSync(activeChanges, passiveChanges, 4, 5);
      }
    } else {
      try {
        ConfigTool.start("set", "-s", "localhost:" + ports.getPort(), "-c", "stripe.1.node.1.tc-properties.com.terracottatech.dynamic-config.simulate=commit-failure");
        fail("Expected to throw exception");
      } catch (IllegalStateException e) {
        Path activePath = Paths.get(getBaseDir() + "/repository/stripe" + stripeId + "/node-" + firstNodeId + "/sanskrit");
        Path passivePath = Paths.get(getBaseDir() + "/repository/stripe" + stripeId + "/node-" + secondNodeId + "/sanskrit");
        List<SanskritObject> activeChanges = AppendLogCapturer.getChanges(activePath);
        List<SanskritObject> passiveChanges = AppendLogCapturer.getChanges(passivePath);
        assertContentsBeforeSync(activeChanges, passiveChanges, 4, 5);
        secondNodeProcess.close();
        startNode("-r", "repository/stripe" + stripeId + "/node-" + secondNodeId);

        waitForAssert(out::getLog, containsString("Passive cannot sync because the change history does not match"));
        passiveChanges = AppendLogCapturer.getChanges(passivePath);
        assertContentsAfterSync(activeChanges, passiveChanges, 4, 5);
      }
    }
  }

  @Test
  public void testPassiveZapsDueToLatestChangeNotCommittedOnPassive() throws Exception {
    int firstNodeId = 1;
    int stripeId = 1;
    int secondNodeId = 2;
    NodeProcess firstNodeProcess = startNode(
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

    waitForAssert(out::getLog, stringContainsInOrder(
        Arrays.asList("Started the server in diagnostic mode", "Started the server in diagnostic mode")
    ));

    ConfigTool.start("attach", "-d", "localhost:" + ports.getPort(), "-s", "localhost:" + ports.getPorts()[2]);
    waitForAssert(out::getLog, containsString("Command successful"));
    out.clearLog();
    ConfigTool.start("activate", "-s", "localhost:" + ports.getPort(), "-n", "tc-cluster", "-l", licensePath().toString());
    waitForAssert(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));
    waitForAssert(out::getLog, containsString("Moved to State[ PASSIVE-STANDBY ]"));

    boolean isFirstNodePassive = false;
    if (firstNodeProcess.getServerState().toString().equals("PASSIVE")) {
      isFirstNodePassive = true;
    }
    if (isFirstNodePassive) {
      try {
        ConfigTool.start("set", "-s", "localhost:" + ports.getPort(), "-c", "stripe.1.node.1.tc-properties.com.terracottatech.dynamic-config.simulate=commit-failure");
        fail("Expected to throw exception");
      } catch (IllegalStateException e) {
        Path passivePath = Paths.get(getBaseDir() + "/repository/stripe" + stripeId + "/node-" + firstNodeId + "/sanskrit");
        Path activePath = Paths.get(getBaseDir() + "/repository/stripe" + stripeId + "/node-" + secondNodeId + "/sanskrit");
        List<SanskritObject> activeChanges = AppendLogCapturer.getChanges(activePath);
        List<SanskritObject> passiveChanges = AppendLogCapturer.getChanges(passivePath);
        assertContentsBeforeSync(activeChanges, passiveChanges, 5, 4);
        firstNodeProcess.close();
        startNode("-r", "repository/stripe" + stripeId + "/node-" + firstNodeId);

        waitForAssert(out::getLog, containsString("Latest configuration change was not committed"));
        passiveChanges = AppendLogCapturer.getChanges(passivePath);
        assertContentsAfterSync(activeChanges, passiveChanges, 5, 4);
      }
    } else {
      try {
        ConfigTool.start("set", "-s", "localhost:" + ports.getPort(), "-c", "stripe.1.node.2.tc-properties.com.terracottatech.dynamic-config.simulate=commit-failure");
        fail("Expected to throw exception");
      } catch (IllegalStateException e) {
        Path activePath = Paths.get(getBaseDir() + "/repository/stripe" + stripeId + "/node-" + firstNodeId + "/sanskrit");
        Path passivePath = Paths.get(getBaseDir() + "/repository/stripe" + stripeId + "/node-" + secondNodeId + "/sanskrit");
        List<SanskritObject> activeChanges = AppendLogCapturer.getChanges(activePath);
        List<SanskritObject> passiveChanges = AppendLogCapturer.getChanges(passivePath);
        assertContentsBeforeSync(activeChanges, passiveChanges, 5, 4);
        secondNodeProcess.close();
        startNode("-r", "repository/stripe" + stripeId + "/node-" + secondNodeId);

        waitForAssert(out::getLog, containsString("Latest configuration change was not committed"));
        passiveChanges = AppendLogCapturer.getChanges(passivePath);
        assertContentsAfterSync(activeChanges, passiveChanges, 5, 4);
      }
    }
  }

  private void assertContentsAfterSync(List<SanskritObject> activeChanges,
                                       List<SanskritObject> passiveChanges,
                                       int activeChangesSize,
                                       int passiveChangesSize
  ) {
    assertThat(activeChanges.size(), is(activeChangesSize));
    assertThat(passiveChanges.size(), is(passiveChangesSize));
    if (activeChangesSize < passiveChangesSize) {
      assertContents(activeChanges, passiveChanges, activeChangesSize);
    } else {
      assertContents(activeChanges, passiveChanges, passiveChangesSize);
    }
  }

  private void assertContentsBeforeSync(List<SanskritObject> activeChanges,
                                        List<SanskritObject> passiveChanges,
                                        int activeChangesSize,
                                        int passiveChangesSize) {
    assertThat(activeChanges.size(), is(activeChangesSize));
    assertThat(passiveChanges.size(), is(passiveChangesSize));
    if (activeChangesSize < passiveChangesSize) {
      assertContents(activeChanges, passiveChanges, activeChangesSize);
    } else {
      assertContents(activeChanges, passiveChanges, passiveChangesSize);
    }
  }

  private void assertContents(List<SanskritObject> activeChanges, List<SanskritObject> passiveChanges, int till) {
    for (int i = 0; i < till; ++i) {
      SanskritObject activeSanskritObject = activeChanges.get(i);
      SanskritObject passiveSanskritObject = passiveChanges.get(i);
      assertEquals(activeSanskritObject.getString(MODE), passiveSanskritObject.getString(MODE));
      assertEquals(activeSanskritObject.getLong(MUTATIVE_MESSAGE_COUNT), passiveSanskritObject.getLong(MUTATIVE_MESSAGE_COUNT));
      assertEquals(activeSanskritObject.getString(LATEST_CHANGE_UUID), passiveSanskritObject.getString(LATEST_CHANGE_UUID));
      if (activeSanskritObject.getString(LATEST_CHANGE_UUID) != null) {
        SanskritObject activeChangeObject = activeSanskritObject.getObject(activeSanskritObject.getString(LATEST_CHANGE_UUID));
        SanskritObject passiveChangeObject = passiveSanskritObject.getObject(passiveSanskritObject.getString(LATEST_CHANGE_UUID));
        assertEquals(activeChangeObject.getString(CHANGE_STATE), passiveChangeObject.getString(CHANGE_STATE));
        assertEquals(activeChangeObject.getLong(CHANGE_VERSION), passiveChangeObject.getLong(CHANGE_VERSION));
        assertEquals(activeChangeObject.getString(PREV_CHANGE_UUID), passiveChangeObject.getString(PREV_CHANGE_UUID));
        SanskritObject activeOpsObject = activeChangeObject.getObject(CHANGE_OPERATION);
        SanskritObject passiveOpsObject = passiveChangeObject.getObject(CHANGE_OPERATION);
        assertEquals(activeOpsObject.getString("type"), passiveOpsObject.getString("type"));
        assertEquals(activeOpsObject.getString("summary"), passiveOpsObject.getString("summary"));
      }
    }
  }

  private void waitForAssert(Callable<String> callable, Matcher<? super String> matcher) {
    waitedAssert(callable, matcher, TIMEOUT);
  }
}

