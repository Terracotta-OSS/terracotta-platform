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
import java.util.List;
import java.util.concurrent.Callable;

import static com.terracottatech.dynamic_config.nomad.persistence.NomadSanskritKeys.CHANGE_OPERATION;
import static com.terracottatech.dynamic_config.nomad.persistence.NomadSanskritKeys.CHANGE_STATE;
import static com.terracottatech.dynamic_config.nomad.persistence.NomadSanskritKeys.CHANGE_VERSION;
import static com.terracottatech.dynamic_config.nomad.persistence.NomadSanskritKeys.LATEST_CHANGE_UUID;
import static com.terracottatech.dynamic_config.nomad.persistence.NomadSanskritKeys.MODE;
import static com.terracottatech.dynamic_config.nomad.persistence.NomadSanskritKeys.MUTATIVE_MESSAGE_COUNT;
import static com.terracottatech.dynamic_config.nomad.persistence.NomadSanskritKeys.PREV_CHANGE_UUID;
import static java.util.Arrays.asList;
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
    Shape shape = createCluster();
    shape.passive.close();

    out.clearLog();
    ConfigTool.start("set", "-s", "localhost:" + shape.activePort, "-c", "offheap-resources.main=1GB");
    assertCommandSuccessful();

    Path activePath = Paths.get(getBaseDir() + "/repository/stripe1/node-" + shape.activeId + "/sanskrit");
    Path passivePath = Paths.get(getBaseDir() + "/repository/stripe1/node-" + shape.passiveId + "/sanskrit");
    List<SanskritObject> activeChanges = AppendLogCapturer.getChanges(activePath);
    List<SanskritObject> passiveChanges = AppendLogCapturer.getChanges(passivePath);
    assertContentsBeforeOrAfterSync(activeChanges, passiveChanges, 5, 3);

    out.clearLog();
    startNode("-r", "repository/stripe1/node-" + shape.passiveId);
    waitForAssert(out::getLog, containsString("Moved to State[ PASSIVE-STANDBY ]"));

    passiveChanges = AppendLogCapturer.getChanges(passivePath);
    assertContentsBeforeOrAfterSync(activeChanges, passiveChanges, 5, 5);
  }

  @Test
  public void testPassiveZapsWhenActiveHasSomeUnCommittedChanges() throws Exception {
    Shape shape = createCluster();
    shape.passive.close();

    // trigger commit failure on active
    // the passive should zap when restarting
    try {
      out.clearLog();
      ConfigTool.start("set", "-s", "localhost:" + shape.activePort, "-c", "stripe.1.node." + shape.activeId + ".tc-properties.com.terracottatech.dynamic-config.simulate=commit-failure");
      fail("Expected to throw exception");
    } catch (IllegalStateException e) {
      e.printStackTrace(System.out);
    }

    Path activePath = Paths.get(getBaseDir() + "/repository/stripe1/node-" + shape.activeId + "/sanskrit");
    Path passivePath = Paths.get(getBaseDir() + "/repository/stripe1/node-" + shape.passiveId + "/sanskrit");
    List<SanskritObject> activeChanges = AppendLogCapturer.getChanges(activePath);
    List<SanskritObject> passiveChanges = AppendLogCapturer.getChanges(passivePath);
    assertContentsBeforeOrAfterSync(activeChanges, passiveChanges, 4, 3);

    out.clearLog();
    startNode("-r", "repository/stripe1/node-" + shape.passiveId);
    waitForAssert(out::getLog, containsString("Active has some PREPARED configuration changes that is not yet committed."));

    passiveChanges = AppendLogCapturer.getChanges(passivePath);
    assertContentsBeforeOrAfterSync(activeChanges, passiveChanges, 4, 3);
  }

  @Test
  public void testPassiveZapsAppendLogHistoryMismatch() throws Exception {
    Shape shape = createCluster();

    // trigger commit failure on active
    // but passive is fine
    // when passive restarts, its history is greater and not equal to the active, so it zaps
    try {
      ConfigTool.start("set", "-s", "localhost:" + shape.activePort, "-c", "stripe.1.node." + shape.activeId + ".tc-properties.com.terracottatech.dynamic-config.simulate=commit-failure");
      fail("Expected to throw exception");
    } catch (IllegalStateException e) {
      e.printStackTrace(System.out);
    }

    Path passivePath = Paths.get(getBaseDir() + "/repository/stripe1/node-" + shape.passiveId + "/sanskrit");
    Path activePath = Paths.get(getBaseDir() + "/repository/stripe1/node-" + shape.activeId + "/sanskrit");
    List<SanskritObject> activeChanges = AppendLogCapturer.getChanges(activePath);
    List<SanskritObject> passiveChanges = AppendLogCapturer.getChanges(passivePath);
    assertContentsBeforeOrAfterSync(activeChanges, passiveChanges, 4, 5);

    shape.passive.close();

    out.clearLog();
    startNode("-r", "repository/stripe1/node-" + shape.passiveId);
    waitForAssert(out::getLog, containsString("Passive cannot sync because the configuration change history does not match"));

    passiveChanges = AppendLogCapturer.getChanges(passivePath);
    assertContentsBeforeOrAfterSync(activeChanges, passiveChanges, 4, 5);
  }

  @Test
  public void testPassiveCanSyncAndRepairIfLatestChangeNotCommitted() throws Exception {
    Shape shape = createCluster();

    // run a non committed configuration change on the passive
    // the active is OK
    // the passive should restart fine
    try {
      ConfigTool.start("set", "-s", "localhost:" + shape.passivePort, "-c", "stripe.1.node." + shape.passiveId + ".tc-properties.com.terracottatech.dynamic-config.simulate=recover-needed");
      fail("Expected to throw exception");
    } catch (IllegalStateException e) {
      e.printStackTrace(System.out);
    }

    Path passivePath = Paths.get(getBaseDir() + "/repository/stripe1/node-" + shape.passiveId + "/sanskrit");
    Path activePath = Paths.get(getBaseDir() + "/repository/stripe1/node-" + shape.activeId + "/sanskrit");
    List<SanskritObject> activeChanges = AppendLogCapturer.getChanges(activePath);
    List<SanskritObject> passiveChanges = AppendLogCapturer.getChanges(passivePath);
    assertContentsBeforeOrAfterSync(activeChanges, passiveChanges, 5, 4);

    shape.passive.close();

    out.clearLog();
    startNode("-r", "repository/stripe1/node-" + shape.passiveId);
    waitForAssert(out::getLog, containsString("Moved to State[ PASSIVE-STANDBY ]"));

    passiveChanges = AppendLogCapturer.getChanges(passivePath);
    assertContentsBeforeOrAfterSync(activeChanges, passiveChanges, 5, 5);
  }

  private void assertContentsBeforeOrAfterSync(List<SanskritObject> activeChanges,
                                               List<SanskritObject> passiveChanges,
                                               int activeChangesSize,
                                               int passiveChangesSize
  ) {
    assertThat(activeChanges.size(), is(activeChangesSize));
    assertThat(passiveChanges.size(), is(passiveChangesSize));
    assertContents(activeChanges, passiveChanges, Math.min(activeChangesSize, passiveChangesSize));
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

  private Shape createCluster() throws Exception {
    NodeProcess[] nodes = {
        startNode(
            "--node-name", "node-1",
            "--node-hostname", "localhost",
            "--node-port", String.valueOf(ports.getPort()),
            "--node-group-port", String.valueOf(ports.getPorts()[1]),
            "--node-log-dir", "logs/stripe1/node-1",
            "--node-metadata-dir", "metadata/stripe1",
            "--node-repository-dir", "repository/stripe1/node-1",
            "--data-dirs", "main:user-data/main/stripe1"),
        startNode(
            "--node-name", "node-2",
            "--node-hostname", "localhost",
            "--node-port", String.valueOf(ports.getPorts()[2]),
            "--node-group-port", String.valueOf(ports.getPorts()[3]),
            "--node-log-dir", "logs/stripe1/node-2",
            "--node-metadata-dir", "metadata/stripe1",
            "--node-repository-dir", "repository/stripe1/node-2",
            "--data-dirs", "main:user-data/main/stripe1")
    };

    waitForAssert(out::getLog, stringContainsInOrder(asList("Started the server in diagnostic mode", "Started the server in diagnostic mode")));

    out.clearLog();
    ConfigTool.start("attach", "-d", "localhost:" + ports.getPort(), "-s", "localhost:" + ports.getPorts()[2]);
    assertCommandSuccessful();

    out.clearLog();
    ConfigTool.start("activate", "-s", "localhost:" + ports.getPort(), "-n", "tc-cluster", "-l", licensePath().toString());
    assertCommandSuccessful();

    Shape shape = new Shape();
    if (nodes[0].getServerState().isPassive()) {
      shape.passive = nodes[0];
      shape.passiveId = 1;
      shape.passivePort = ports.getPorts()[0];
      shape.active = nodes[1];
      shape.activeId = 2;
      shape.activePort = ports.getPorts()[2];
    } else {
      shape.passive = nodes[1];
      shape.passiveId = 2;
      shape.passivePort = ports.getPorts()[2];
      shape.active = nodes[0];
      shape.activeId = 1;
      shape.activePort = ports.getPorts()[0];
    }
    return shape;
  }

  static class Shape {
    int activeId;
    int passiveId;

    int activePort;
    int passivePort;

    NodeProcess active;
    NodeProcess passive;
  }
}

