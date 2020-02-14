/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.system_tests;

import org.junit.Before;
import org.junit.Test;
import org.terracotta.angela.common.tcconfig.TerracottaServer;
import org.terracotta.dynamic_config.system_tests.util.AppendLogCapturer;
import org.terracotta.persistence.sanskrit.SanskritException;
import org.terracotta.persistence.sanskrit.SanskritObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
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
import static org.terracotta.dynamic_config.system_tests.util.AngelaMatchers.hasExitStatus;

@ClusterDefinition(nodesPerStripe = 2, autoActivate = true)
public class ConfigSyncIT extends DynamicConfigIT {
  private int activeNodeId;
  private int passiveNodeId;

  @Before
  @Override
  public void before() {
    super.before();
    if (tsa.getActive() == getNode(1, 1)) {
      activeNodeId = 1;
      passiveNodeId = 2;
    } else {
      activeNodeId = 2;
      passiveNodeId = 1;
    }
  }

  @Test
  public void testPassiveSyncingAppendChangesFromActive() throws Exception {
    tsa.stop(getNode(1, passiveNodeId));
    assertThat(tsa.getStopped().size(), is(1));
    out.clearLog();

    configToolInvocation("set", "-s", "localhost:" + getNodePort(1, activeNodeId), "-c", "offheap-resources.main=1GB");
    assertCommandSuccessful();

    //TODO TDB-4842: The stop and corresponding start is needed to prevent IOException on Windows
    // Passive is already stopped, so only shutdown and restart the active
    tsa.stop(getNode(1, activeNodeId));
    assertThat(tsa.getStopped().size(), is(2));
    assertContentsBeforeOrAfterSync(5, 3);
    tsa.start(getNode(1, activeNodeId));
    assertThat(tsa.getActives().size(), is(1));

    out.clearLog();
    tsa.start(getNode(1, passiveNodeId));
    waitUntil(out::getLog, containsString("Moved to State[ PASSIVE-STANDBY ]"));

    //TODO TDB-4842: The stop is needed to prevent IOException on Windows
    tsa.stopAll();
    assertContentsBeforeOrAfterSync(5, 5);
  }

  @Test
  public void testPassiveZapsWhenActiveHasSomeUnCommittedChanges() throws Exception {
    tsa.stop(getNode(1, passiveNodeId));
    assertThat(tsa.getStopped().size(), is(1));

    // trigger commit failure on active
    // the passive should zap when restarting
    assertThat(
        configToolInvocation("set", "-s", "localhost:" + getNodePort(1, activeNodeId), "-c", "stripe.1.node." + activeNodeId + ".tc-properties.org.terracotta.dynamic-config.simulate=commit-failure"),
        not(hasExitStatus(0)));

    //TODO TDB-4842: The stop and corresponding start is needed to prevent IOException on Windows
    // Passive is already stopped, so only shutdown and restart the active
    tsa.stop(getNode(1, activeNodeId));
    assertThat(tsa.getStopped().size(), is(2));
    assertContentsBeforeOrAfterSync(4, 3);
    tsa.start(getNode(1, activeNodeId));
    assertThat(tsa.getActives().size(), is(1));

    out.clearLog();
    try {
      tsa.start(getNode(1, passiveNodeId));
      fail();
    } catch (Exception e) {
      waitUntil(out::getLog, containsString("Active has some PREPARED configuration changes that are not yet committed."));
    }

    //TODO TDB-4842: The stop is needed to prevent IOException on Windows
    tsa.stopAll();
    assertContentsBeforeOrAfterSync(4, 3);
  }

  @Test
  public void testPassiveZapsAppendLogHistoryMismatch() throws Exception {
    // trigger commit failure on active
    // but passive is fine
    // when passive restarts, its history is greater and not equal to the active, so it zaps
    assertThat(
        configToolInvocation("set", "-s", "localhost:" + getNodePort(1, activeNodeId), "-c", "stripe.1.node." + activeNodeId + ".tc-properties.org.terracotta.dynamic-config.simulate=commit-failure"),
        not(hasExitStatus(0)));

    //TODO TDB-4842: The stop and corresponding start is needed to prevent IOException on Windows
    tsa.stop(getNode(1, passiveNodeId));
    tsa.stop(getNode(1, activeNodeId));
    assertThat(tsa.getStopped().size(), is(2));
    assertContentsBeforeOrAfterSync(4, 5);
    // Start only the former active for now (the passive startup would be done later, and should fail)
    tsa.start(getNode(1, activeNodeId));
    assertThat(tsa.getActives().size(), is(1));
    out.clearLog();

    try {
      tsa.start(getNode(1, passiveNodeId));
      fail();
    } catch (Exception e) {
      waitUntil(out::getLog, containsString("Passive cannot sync because the configuration change history does not match"));
    }

    //TODO TDB-4842: The stop is needed to prevent IOException on Windows
    tsa.stopAll();
    assertContentsBeforeOrAfterSync(4, 5);
  }

  @Test
  public void testPassiveCanSyncAndRepairIfLatestChangeNotCommitted() throws Exception {
    // run a non committed configuration change on the passive
    // the active is OK
    // the passive should restart fine
    assertThat(
        configToolInvocation("set", "-s", "localhost:" + getNodePort(1, passiveNodeId), "-c", "stripe.1.node." + passiveNodeId + ".tc-properties.org.terracotta.dynamic-config.simulate=recover-needed"),
        not(hasExitStatus(0)));

    //TODO TDB-4842: The stop is needed to prevent IOException on Windows
    tsa.stop(getNode(1, passiveNodeId));
    tsa.stop(getNode(1, activeNodeId));
    assertThat(tsa.getStopped().size(), is(2));
    assertContentsBeforeOrAfterSync(5, 4);
    tsa.start(getNode(1, activeNodeId));

    tsa.start(getNode(1, passiveNodeId));
    waitUntil(out::getLog, containsString("Moved to State[ PASSIVE-STANDBY ]"));

    //TODO TDB-4842: The stop is needed to prevent IOException on Windows
    tsa.stopAll();
    assertContentsBeforeOrAfterSync(5, 5);
  }

  private void assertContentsBeforeOrAfterSync(int activeChangesSize, int passiveChangesSize) throws SanskritException, IOException {
    TerracottaServer active = getNode(1, activeNodeId);
    TerracottaServer passive = getNode(1, passiveNodeId);

    Path activePath = getBaseDir().resolve("activeRepo");
    Path passivePath = getBaseDir().resolve("passiveRepo");
    Files.createDirectories(activePath);
    Files.createDirectories(passivePath);

    tsa.browse(active, Paths.get(active.getConfigRepo()).resolve("sanskrit").toString()).downloadTo(activePath.toFile());
    tsa.browse(passive, Paths.get(passive.getConfigRepo()).resolve("sanskrit").toString()).downloadTo(passivePath.toFile());

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

