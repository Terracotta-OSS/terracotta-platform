/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.system_tests;

import org.junit.Before;
import org.junit.Test;
import org.terracotta.dynamic_config.cli.config_tool.ConfigTool;
import org.terracotta.dynamic_config.system_tests.util.AppendLogCapturer;
import org.terracotta.persistence.sanskrit.SanskritException;
import org.terracotta.persistence.sanskrit.SanskritObject;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.terracotta.dynamic_config.server.nomad.persistence.NomadSanskritKeys.CHANGE_OPERATION;
import static org.terracotta.dynamic_config.server.nomad.persistence.NomadSanskritKeys.CHANGE_STATE;
import static org.terracotta.dynamic_config.server.nomad.persistence.NomadSanskritKeys.CHANGE_VERSION;
import static org.terracotta.dynamic_config.server.nomad.persistence.NomadSanskritKeys.LATEST_CHANGE_UUID;
import static org.terracotta.dynamic_config.server.nomad.persistence.NomadSanskritKeys.MODE;
import static org.terracotta.dynamic_config.server.nomad.persistence.NomadSanskritKeys.MUTATIVE_MESSAGE_COUNT;
import static org.terracotta.dynamic_config.server.nomad.persistence.NomadSanskritKeys.PREV_CHANGE_UUID;

@ClusterDefinition(nodesPerStripe = 2, autoActivate = true)
public class ConfigSyncIT extends DynamicConfigIT {
  private int activeNodeId;
  private int passiveNodeId;

  @Before
  @Override
  public void before() throws Exception {
    super.before();
    if (getNodeProcess(1, 1).getServerState().isActive()) {
      activeNodeId = 1;
      passiveNodeId = 2;
    } else {
      activeNodeId = 2;
      passiveNodeId = 1;
    }
  }

  @Test
  public void testPassiveSyncingAppendChangesFromActive() throws Exception {
    getNodeProcess(1, passiveNodeId).close();

    out.clearLog();
    ConfigTool.start("set", "-s", "localhost:" + getNodePort(1, activeNodeId), "-c", "offheap-resources.main=1GB");
    assertCommandSuccessful();
    assertContentsBeforeOrAfterSync(5, 3);

    out.clearLog();
    startNode(1, passiveNodeId);
    waitUntil(out::getLog, containsString("Moved to State[ PASSIVE-STANDBY ]"));
    assertContentsBeforeOrAfterSync(5, 5);
  }

  @Test
  public void testPassiveZapsWhenActiveHasSomeUnCommittedChanges() throws Exception {
    getNodeProcess(1, passiveNodeId).close();

    // trigger commit failure on active
    // the passive should zap when restarting
    try {
      out.clearLog();
      ConfigTool.start("set", "-s", "localhost:" + getNodePort(1, activeNodeId), "-c", "stripe.1.node." + activeNodeId + ".tc-properties.org.terracotta.dynamic-config.simulate=commit-failure");
      fail("Expected to throw exception");
    } catch (IllegalStateException e) {
      e.printStackTrace(System.out);
    }

    assertContentsBeforeOrAfterSync(4, 3);

    out.clearLog();
    startNode(1, passiveNodeId);
    waitUntil(out::getLog, containsString("Active has some PREPARED configuration changes that are not yet committed."));
    assertContentsBeforeOrAfterSync(4, 3);
  }

  @Test
  public void testPassiveZapsAppendLogHistoryMismatch() throws Exception {
    // trigger commit failure on active
    // but passive is fine
    // when passive restarts, its history is greater and not equal to the active, so it zaps
    try {
      ConfigTool.start("set", "-s", "localhost:" + getNodePort(1, activeNodeId), "-c", "stripe.1.node." + activeNodeId + ".tc-properties.org.terracotta.dynamic-config.simulate=commit-failure");
      fail("Expected to throw exception");
    } catch (IllegalStateException e) {
      e.printStackTrace(System.out);
    }

    assertContentsBeforeOrAfterSync(4, 5);

    getNodeProcess(1, passiveNodeId).close();

    out.clearLog();
    startNode(1, passiveNodeId);
    waitUntil(out::getLog, containsString("Passive cannot sync because the configuration change history does not match"));
    assertContentsBeforeOrAfterSync(4, 5);
  }

  @Test
  public void testPassiveCanSyncAndRepairIfLatestChangeNotCommitted() throws Exception {
    // run a non committed configuration change on the passive
    // the active is OK
    // the passive should restart fine
    try {
      ConfigTool.start("set", "-s", "localhost:" + getNodePort(1, passiveNodeId), "-c", "stripe.1.node." + passiveNodeId + ".tc-properties.org.terracotta.dynamic-config.simulate=recover-needed");
      fail("Expected to throw exception");
    } catch (IllegalStateException e) {
      e.printStackTrace(System.out);
    }

    assertContentsBeforeOrAfterSync(5, 4);

    getNodeProcess(1, passiveNodeId).close();

    out.clearLog();
    startNode(1, passiveNodeId);
    waitUntil(out::getLog, containsString("Moved to State[ PASSIVE-STANDBY ]"));
    assertContentsBeforeOrAfterSync(5, 5);
  }

  private void assertContentsBeforeOrAfterSync(int activeChangesSize, int passiveChangesSize) throws SanskritException {
    Path activePath = Paths.get(getBaseDir() + "/repository/stripe1/node-" + activeNodeId + "/sanskrit");
    Path passivePath = Paths.get(getBaseDir() + "/repository/stripe1/node-" + passiveNodeId + "/sanskrit");

    List<SanskritObject> activeChanges = AppendLogCapturer.getChanges(activePath);
    List<SanskritObject> passiveChanges = AppendLogCapturer.getChanges(passivePath);

    assertThat(activeChanges.size(), is(activeChangesSize));
    assertThat(passiveChanges.size(), is(passiveChangesSize));

    for (int i = 0, till = Math.min(activeChangesSize, passiveChangesSize); i < till; ++i) {
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
}

