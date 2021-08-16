/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.dynamic_config.system_tests.activated;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.terracotta.angela.common.tcconfig.TerracottaServer;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;
import org.terracotta.persistence.sanskrit.JsonUtils;
import org.terracotta.persistence.sanskrit.MutableSanskritObject;
import org.terracotta.persistence.sanskrit.ObjectMapperSupplier;
import org.terracotta.persistence.sanskrit.SanskritException;
import org.terracotta.persistence.sanskrit.SanskritImpl;
import org.terracotta.persistence.sanskrit.SanskritObject;
import org.terracotta.persistence.sanskrit.SanskritObjectImpl;
import org.terracotta.persistence.sanskrit.file.FileBasedFilesystemDirectory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.successful;
import static org.terracotta.dynamic_config.server.configuration.nomad.persistence.NomadSanskritKeys.CHANGE_OPERATION;
import static org.terracotta.dynamic_config.server.configuration.nomad.persistence.NomadSanskritKeys.CHANGE_STATE;
import static org.terracotta.dynamic_config.server.configuration.nomad.persistence.NomadSanskritKeys.CHANGE_VERSION;
import static org.terracotta.dynamic_config.server.configuration.nomad.persistence.NomadSanskritKeys.LATEST_CHANGE_UUID;
import static org.terracotta.dynamic_config.server.configuration.nomad.persistence.NomadSanskritKeys.MODE;
import static org.terracotta.dynamic_config.server.configuration.nomad.persistence.NomadSanskritKeys.MUTATIVE_MESSAGE_COUNT;
import static org.terracotta.dynamic_config.server.configuration.nomad.persistence.NomadSanskritKeys.PREV_CHANGE_UUID;

@ClusterDefinition(nodesPerStripe = 2, autoActivate = true)
public class ConfigSyncIT extends DynamicConfigIT {

  private int activeNodeId;
  private int passiveNodeId;

  public ConfigSyncIT() {
    super(Duration.ofSeconds(180));
  }

  @Before
  public void before() {
    if (angela.tsa().getActive() == getNode(1, 1)) {
      activeNodeId = 1;
      passiveNodeId = 2;
    } else {
      activeNodeId = 2;
      passiveNodeId = 1;
    }
  }

  @Test
  public void testPassiveSyncingAppendChangesFromActive() throws Exception {
    stopNode(1, passiveNodeId);
    assertThat(angela.tsa().getStopped().size(), is(1));

    assertThat(configTool("set", "-s", "localhost:" + getNodePort(1, activeNodeId), "-c", "offheap-resources.main=1GB"), is(successful()));

    //TODO TDB-4842: The stop and corresponding start is needed to prevent IOException on Windows
    // Passive is already stopped, so only shutdown and restart the active
    stopNode(1, activeNodeId);
    assertThat(angela.tsa().getStopped().size(), is(2));
    assertContentsAfterRestart(5, 3);
    angela.tsa().start(getNode(1, activeNodeId));
    waitForActive(1);
    assertThat(angela.tsa().getActives().size(), is(1));

    angela.tsa().start(getNode(1, passiveNodeId));
    waitForPassive(1, passiveNodeId);

    verifyTopologies();

    //TODO TDB-4842: The stop is needed to prevent IOException on Windows
    angela.tsa().stopAll();
    assertContentsAfterRestart(5, 5);
  }

  @Test
  public void testPassiveSyncWhenActiveHasSomeUnCommittedChanges() throws Exception {
    stopNode(1, passiveNodeId);
    assertThat(angela.tsa().getStopped().size(), is(1));

    // triggers a permanent failure during Nomad commit phase only on active node
    // active entity will return with the failure
    // passive entity will not fail and commit
    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(1, activeNodeId), "-c", "stripe.1.node." + activeNodeId + ".logger-overrides.org.terracotta.dynamic-config.simulate=INFO"),
        is(not(successful())));

    //TODO TDB-4842: The stop and corresponding start is needed to prevent IOException on Windows
    // Passive is already stopped, so only shutdown and restart the active
    stopNode(1, activeNodeId);
    assertThat(angela.tsa().getStopped().size(), is(2));
    assertContentsAfterRestart(4, 3);
    angela.tsa().start(getNode(1, activeNodeId));
    waitForActive(1);
    assertThat(angela.tsa().getActives().size(), is(1));

    angela.tsa().start(getNode(1, passiveNodeId));
    waitForPassive(1, passiveNodeId);

    //TODO TDB-4842: The stop is needed to prevent IOException on Windows
    angela.tsa().stopAll();
    assertContentsAfterRestart(4, 4);
  }

  @Test
  public void testPassiveRestartsIfPartialCommitOnActive() throws Exception {
    // trigger commit failure on active
    // but passive is fine
    // when passive restarts, its history is greater and not equal to the active, so it zaps
    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(1, activeNodeId), "-c", "stripe.1.node." + activeNodeId + ".logger-overrides.org.terracotta.dynamic-config.simulate=INFO"),
        is(not(successful())));

    //TODO TDB-4842: The stop and corresponding start is needed to prevent IOException on Windows

    // wait for some time to ensure the Nomad Message gets applied to passive
    Thread.sleep(5000);
    stopNode(1, activeNodeId);
    stopNode(1, passiveNodeId);
    assertThat(angela.tsa().getStopped().size(), is(2));
    assertContentsAfterRestart(4, 5);
    // Start only the former active for now (the passive startup would be done later, and should fail)
    angela.tsa().start(getNode(1, activeNodeId));
    waitForActive(1);
    assertThat(angela.tsa().getActives().size(), is(1));

    angela.tsa().start(getNode(1, passiveNodeId));
    waitForPassive(1, passiveNodeId);

    //TODO TDB-4842: The stop is needed to prevent IOException on Windows
    angela.tsa().stopAll();
    assertContentsAfterRestart(4, 5);
  }

  @Test
  public void testPassiveCanSyncAndRepairIfLatestChangeNotCommitted() throws Exception {
    // triggers a failure during Nomad commit phase only on passive node
    // active entity will return with no failure
    // passive entity will fail and restart the passive server
    // passive server will sync and repair itself
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(1, activeNodeId), "-c", "stripe.1.node." + passiveNodeId + ".logger-overrides.org.terracotta.dynamic-config.simulate=DEBUG"), is(successful()));

    waitUntilServerStdOut(getNode(1, passiveNodeId), "Requesting restart");

    // passive should restart and sync again to repair its non committed change
    waitForPassive(1, passiveNodeId);

    // nomad system should commit last change
    waitUntil(
        () -> usingTopologyService(1, passiveNodeId, TopologyService::hasIncompleteChange),
        is(false));

    // passive node should have the key/value at runtime now
    waitUntil(
        () -> usingTopologyService(1, passiveNodeId, topologyService -> topologyService.getRuntimeNodeContext().getNode().getLoggerOverrides().orDefault()),
        hasEntry("org.terracotta.dynamic-config.simulate", "DEBUG"));

    //TODO TDB-4842: The stop is needed to prevent IOException on Windows
    stopNode(1, passiveNodeId);
    stopNode(1, activeNodeId);
    assertThat(angela.tsa().getStopped().size(), is(2));
    assertContentsAfterRestart(5, 5);
    angela.tsa().start(getNode(1, activeNodeId));
    angela.tsa().start(getNode(1, passiveNodeId));
    waitForPassive(1, passiveNodeId);

    verifyTopologies();

    //TODO TDB-4842: The stop is needed to prevent IOException on Windows
    angela.tsa().stopAll();
    assertContentsAfterRestart(5, 5);
  }

  private void assertContentsAfterRestart(int activeChangesSize, int passiveChangesSize) throws SanskritException, IOException {
    TerracottaServer active = getNode(1, activeNodeId);
    TerracottaServer passive = getNode(1, passiveNodeId);

    Path activePath = getBaseDir().resolve("activeRepo");
    Path passivePath = getBaseDir().resolve("passiveRepo");
    Files.createDirectories(activePath);
    Files.createDirectories(passivePath);

    angela.tsa().browse(active, Paths.get(active.getConfigRepo()).resolve("changes").toString()).downloadTo(activePath.toFile());
    angela.tsa().browse(passive, Paths.get(passive.getConfigRepo()).resolve("changes").toString()).downloadTo(passivePath.toFile());

    List<SanskritObject> activeChanges = getChanges(activePath);
    List<SanskritObject> passiveChanges = getChanges(passivePath);

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

  private void verifyTopologies() throws Exception {
    // config repos written on disk should be the same
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 1)), is(equalTo(getUpcomingCluster("localhost", getNodePort(1, 2)))));
    // runtime topology should be the same
    assertThat(getRuntimeCluster("localhost", getNodePort(1, 1)), is(equalTo(getRuntimeCluster("localhost", getNodePort(1, 2)))));
    // runtime topology should be the same as upcoming one
    assertThat(getRuntimeCluster("localhost", getNodePort(1, 1)), is(equalTo(getUpcomingCluster("localhost", getNodePort(1, 2)))));
  }

  private List<SanskritObject> getChanges(Path pathToAppendLog) throws SanskritException {
    ObjectMapper objectMapper = objectMapperFactory.create();
    List<SanskritObject> res = new ArrayList<>();
    new SanskritImpl(new FileBasedFilesystemDirectory(pathToAppendLog), ObjectMapperSupplier.notVersioned(objectMapper)) {
      @Override
      public void onNewRecord(String timeStamp, String json) throws SanskritException {
        MutableSanskritObject mutableSanskritObject = new SanskritObjectImpl(ObjectMapperSupplier.notVersioned(objectMapper));
        JsonUtils.parse(ObjectMapperSupplier.notVersioned(objectMapper), null, json, mutableSanskritObject);
        res.add(mutableSanskritObject);
      }
    };
    return res;
  }
}

