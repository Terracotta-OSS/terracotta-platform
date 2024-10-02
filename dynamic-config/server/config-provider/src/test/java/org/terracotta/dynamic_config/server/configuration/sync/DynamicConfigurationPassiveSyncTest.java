/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
package org.terracotta.dynamic_config.server.configuration.sync;

import org.junit.Before;
import org.junit.Test;
import org.terracotta.dynamic_config.api.json.DynamicConfigJsonModule;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.Setting;
import org.terracotta.dynamic_config.api.model.Testing;
import org.terracotta.dynamic_config.api.model.nomad.ClusterActivationNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.SettingNomadChange;
import org.terracotta.dynamic_config.api.service.DynamicConfigService;
import org.terracotta.dynamic_config.api.service.NomadChangeInfo;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.dynamic_config.api.server.DynamicConfigNomadServer;
import org.terracotta.json.DefaultJsonFactory;
import org.terracotta.json.Json;
import org.terracotta.nomad.client.change.NomadChange;
import org.terracotta.nomad.messages.ChangeDetails;
import org.terracotta.nomad.messages.CommitMessage;
import org.terracotta.nomad.messages.DiscoverResponse;
import org.terracotta.nomad.messages.PrepareMessage;
import org.terracotta.nomad.messages.RollbackMessage;
import org.terracotta.nomad.server.NomadException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThrows;
import static org.junit.internal.matchers.ThrowableMessageMatcher.hasMessage;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.terracotta.common.struct.MemoryUnit.MB;
import static org.terracotta.dynamic_config.api.model.nomad.Applicability.cluster;
import static org.terracotta.dynamic_config.server.configuration.sync.Require.NOTHING;
import static org.terracotta.dynamic_config.server.configuration.sync.Require.RESTART_REQUIRED;
import static org.terracotta.nomad.messages.AcceptRejectResponse.accept;
import static org.terracotta.nomad.server.ChangeRequestState.COMMITTED;
import static org.terracotta.nomad.server.ChangeRequestState.PREPARED;
import static org.terracotta.nomad.server.ChangeRequestState.ROLLED_BACK;
import static org.terracotta.testing.ExceptionMatcher.throwing;

/**
 * <pre>
 * =========
 * IMPORTANT
 * =========
 * </pre>
 * The numbers in the test methods reflect the use cases described in {@link DynamicConfigNomadSynchronizer}
 */
public class DynamicConfigurationPassiveSyncTest {

  private final Instant now = Instant.now();

  private String activeLicense = null;
  private String passiveLicense = null;

  private final NodeContext activeTopology = new NodeContext(
      Testing.newTestCluster("foo",
          Testing.newTestStripe("stripe-1").addNodes(
              Testing.newTestNode("bar", "localhost"))), Testing.N_UIDS[1]);

  private final NodeContext passiveTopology = activeTopology.clone();

  private NomadChangeInfo activeActivation = committed(randomUUID(), new ClusterActivationNomadChange(activeTopology.getCluster().clone()), 1L);
  private NomadChangeInfo passiveActivation = activeActivation; // joint activation
  private final DynamicConfigSyncData.Codec codec = new DynamicConfigSyncData.Codec(new DefaultJsonFactory().withModule(new DynamicConfigJsonModule()));

  private final TopologyService activeTopologyService = mock(TopologyService.class);
  private final TopologyService passiveTopologyService = mock(TopologyService.class);
  private final DynamicConfigNomadServer activeNomadServer = mock(DynamicConfigNomadServer.class);
  private final DynamicConfigNomadServer passiveNomadServer = mock(DynamicConfigNomadServer.class);
  private final DynamicConfigService activeDynamicConfigService = mock(DynamicConfigService.class);
  private final DynamicConfigService passiveDynamicConfigService = mock(DynamicConfigService.class);

  private final DynamicConfigurationPassiveSync activeSyncManager = new DynamicConfigurationPassiveSync(activeTopology, activeNomadServer, activeDynamicConfigService, activeTopologyService, () -> activeLicense);
  private final DynamicConfigurationPassiveSync passiveSyncManager = new DynamicConfigurationPassiveSync(passiveTopology, passiveNomadServer, passiveDynamicConfigService, passiveTopologyService, () -> passiveLicense);

  private final List<NomadChangeInfo> active = new ArrayList<>();
  private final List<NomadChangeInfo> passive = new ArrayList<>();

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() throws NomadException {
    when(activeTopologyService.getUpcomingNodeContext()).thenReturn(activeTopology);
    when(passiveTopologyService.getUpcomingNodeContext()).thenReturn(passiveTopology);
    when(activeNomadServer.getChangeHistory()).thenReturn(active);
    when(passiveNomadServer.getChangeHistory()).thenReturn(passive);
    when(passiveNomadServer.discover()).thenReturn(mock(DiscoverResponse.class));
    when(passiveNomadServer.prepare(any(PrepareMessage.class))).thenReturn(accept());
    when(passiveNomadServer.commit(any(CommitMessage.class))).thenReturn(accept());
    when(passiveNomadServer.rollback(any(RollbackMessage.class))).thenReturn(accept());
  }

  @Test
  public void testCodec() {
    List<NomadChangeInfo> nomadChanges = asList(
        committed(randomUUID(), change("a", "100MB"), 1),
        committed(randomUUID(), change("a", "200MB"), 2)
    );

    List<NomadChangeInfo> decodedChanges = codec.decode(codec.encode(new DynamicConfigSyncData(nomadChanges, activeTopology.getCluster(), null))).getNomadChanges();
    Json json = new DefaultJsonFactory().withModule(new DynamicConfigJsonModule()).create();
    assertThat(json.map(decodedChanges), is(equalTo(json.map(nomadChanges))));
  }

  @Test
  public void test_sync_license() throws NomadException {
    active.add(activeActivation);
    active.add(committed(randomUUID(), change("a", "100MB"), 2L));

    passive.addAll(active);

    activeLicense = "license content";

    assertThat(passiveSyncManager.sync(codec.decode(codec.encode(activeSyncManager.getSyncData()))), hasItems(NOTHING));
    verify(passiveDynamicConfigService).upgradeLicense("license content");
  }

  @Test
  public void test_0_match_prepared() throws NomadException {
    UUID uuid = randomUUID();

    active.add(activeActivation);
    active.add(prepared(uuid, change("a", "100MB"), 2L));

    passive.add(passiveActivation);
    passive.add(prepared(uuid, change("a", "100MB"), 2L));

    assertThat(sync(), hasItem(NOTHING));

    check(0, 0, 0);
  }

  @Test
  public void test_0_match_prepared_by_hash() throws NomadException {
    active.add(activeActivation);
    active.add(prepared(randomUUID(), change("a", "100MB"), 2L, "hash"));

    passive.add(passiveActivation);
    passive.add(prepared(randomUUID(), change("a", "100MB"), 2L, "hash"));

    assertThat(sync(), hasItem(NOTHING));

    check(0, 0, 0);
  }

  @Test
  public void test_0_mismatch_prepared() throws NomadException {
    NomadChangeInfo active, passive;

    this.active.add(activeActivation);
    this.active.add(active = prepared(randomUUID(), change("a", "100MB"), 2L));

    this.passive.add(passiveActivation);
    this.passive.add(passive = prepared(randomUUID(), change("a", "300MB"), 2L));

    IllegalStateException e = assertThrows(IllegalStateException.class, this::sync);
    assertThat(e, hasMessage(equalTo("Node cannot sync because the configuration change history does not match: " + passive + " does not match source: " + active)));
  }

  @Test
  public void test_1_match_commit() throws NomadException {
    UUID uuid = randomUUID();

    active.add(activeActivation);
    active.add(committed(uuid, change("a", "100MB"), 2L));

    passive.add(passiveActivation);
    passive.add(committed(uuid, change("a", "100MB"), 2L));

    assertThat(sync(), hasItem(NOTHING));

    check(0, 0, 0);
  }

  @Test
  public void test_1_match_commit_by_hash() throws NomadException {
    active.add(activeActivation);
    active.add(committed(randomUUID(), change("a", "100MB"), 2L, "hash"));

    passive.add(passiveActivation);
    passive.add(committed(randomUUID(), change("a", "100MB"), 2L, "hash"));

    assertThat(sync(), hasItem(NOTHING));

    check(0, 0, 0);
  }

  @Test
  public void test_1_mismatch_commit() throws NomadException {
    NomadChangeInfo active, passive;

    this.active.add(activeActivation);
    this.active.add(active = committed(randomUUID(), change("a", "100MB"), 2L));

    this.passive.add(passiveActivation);
    this.passive.add(passive = committed(randomUUID(), change("a", "300MB"), 2L));

    IllegalStateException e = assertThrows(IllegalStateException.class, this::sync);
    assertThat(e, hasMessage(equalTo("Node cannot sync because the configuration change history does not match: " + passive + " does not match source: " + active)));
  }

  @Test
  public void test_2_partial_commit_on_passive() throws NomadException {
    UUID uuid = randomUUID();

    active.add(activeActivation);
    active.add(committed(uuid, change("a", "100MB"), 2L));

    passive.add(passiveActivation);
    passive.add(prepared(uuid, change("a", "100MB"), 2L));

    mockDiscoveryForRepair();

    assertThat(sync(), hasItem(RESTART_REQUIRED));

    check(0, 1, 0);
  }

  @Test
  public void test_2_partial_commit_on_passive_by_hash() throws NomadException {
    active.add(activeActivation);
    active.add(committed(randomUUID(), change("a", "100MB"), 2L, "hash"));

    passive.add(passiveActivation);
    passive.add(prepared(randomUUID(), change("a", "100MB"), 2L, "hash"));

    mockDiscoveryForRepair();

    assertThat(sync(), hasItem(RESTART_REQUIRED));

    check(0, 1, 0);
  }

  @Test
  public void test_3_partial_commit_on_active() throws NomadException {
    UUID uuid = randomUUID();

    active.add(activeActivation);
    active.add(prepared(uuid, change("a", "100MB"), 2L));

    passive.add(passiveActivation);
    passive.add(committed(uuid, change("a", "100MB"), 2L));

    assertThat(sync(), hasItem(NOTHING));

    check(0, 0, 0);
  }

  @Test
  public void test_3_partial_commit_on_active_by_hash() throws NomadException {
    active.add(activeActivation);
    active.add(prepared(randomUUID(), change("a", "100MB"), 2L, "hash"));

    passive.add(passiveActivation);
    passive.add(committed(randomUUID(), change("a", "100MB"), 2L, "hash"));

    assertThat(sync(), hasItem(NOTHING));

    check(0, 0, 0);
  }

  @Test
  public void test_4_rollback_ignored() throws NomadException {
    UUID uuid = randomUUID();

    active.add(activeActivation);
    active.add(rolledBack(uuid, change("a", "100MB"), 2L));

    passive.add(passiveActivation);
    passive.add(rolledBack(uuid, change("a", "100MB"), 2L));

    assertThat(sync(), hasItem(NOTHING));

    check(0, 0, 0);
  }

  @Test
  public void test_4_rollback_ignored_by_hash() throws NomadException {
    active.add(activeActivation);
    active.add(rolledBack(randomUUID(), change("a", "100MB"), 2L, "hash"));

    passive.add(passiveActivation);
    passive.add(rolledBack(randomUUID(), change("a", "100MB"), 2L, "hash"));

    assertThat(sync(), hasItem(NOTHING));

    check(0, 0, 0);
  }

  @Test
  public void test_5_partial_rollback_on_passive() throws NomadException {
    UUID uuid = randomUUID();

    active.add(activeActivation);
    active.add(rolledBack(uuid, change("a", "100MB"), 2L));

    passive.add(passiveActivation);
    passive.add(prepared(uuid, change("a", "100MB"), 2L));

    mockDiscoveryForRepair();

    assertThat(sync(), hasItem(NOTHING));

    check(0, 0, 1);
  }

  @Test
  public void test_5_partial_rollback_on_passive_by_hash() throws NomadException {
    active.add(activeActivation);
    active.add(rolledBack(randomUUID(), change("a", "100MB"), 2L, "hash"));

    passive.add(passiveActivation);
    passive.add(prepared(randomUUID(), change("a", "100MB"), 2L, "hash"));

    mockDiscoveryForRepair();

    assertThat(sync(), hasItem(NOTHING));

    check(0, 0, 1);
  }

  @Test
  public void test_6_partial_rollback_on_active() throws NomadException {
    UUID uuid = randomUUID();

    active.add(activeActivation);
    active.add(prepared(uuid, change("a", "100MB"), 2L));

    passive.add(passiveActivation);
    passive.add(rolledBack(uuid, change("a", "100MB"), 2L));

    assertThat(sync(), hasItem(NOTHING));

    check(0, 0, 0);
  }

  @Test
  public void test_7_inconsistency() throws NomadException {
    UUID uuid = randomUUID();
    NomadChangeInfo committed;

    active.add(activeActivation);
    active.add(committed = committed(uuid, change("a", "100MB"), 2L));

    passive.add(passiveActivation);
    passive.add(rolledBack(uuid, change("a", "100MB"), 2L));

    IllegalStateException e = assertThrows(IllegalStateException.class, this::sync);
    assertThat(e, hasMessage(equalTo("Node cannot sync because the configuration change history does not match: " + committed + " has been rolled back on this node")));
  }

  @Test
  public void test_8_inconsistency() throws NomadException {
    UUID uuid = randomUUID();
    NomadChangeInfo committed;

    active.add(activeActivation);
    active.add(rolledBack(uuid, change("a", "100MB"), 2L));

    passive.add(passiveActivation);
    passive.add(committed = committed(uuid, change("a", "100MB"), 2L));

    IllegalStateException e = assertThrows(IllegalStateException.class, this::sync);
    assertThat(e, hasMessage(equalTo("Node cannot sync because the configuration change history does not match: " + committed + " has been rolled back on the source")));
  }

  @Test
  public void test_9_passive_ahead() throws NomadException {
    UUID uuid = randomUUID();
    NomadChangeInfo ahead;

    active.add(activeActivation);
    active.add(committed(uuid, change("a", "100MB"), 2L));

    passive.add(passiveActivation);
    passive.add(committed(uuid, change("a", "100MB"), 2L));
    passive.add(ahead = committed(randomUUID(), change("b", "200MB"), 3L));

    IllegalStateException e = assertThrows(IllegalStateException.class, this::sync);
    assertThat(e, hasMessage(equalTo("Node cannot sync because the configuration change history does not match: this node is ahead of the source: " + ahead)));
  }

  @Test
  public void test_10_partial_prepare_on_passive() throws NomadException {
    UUID uuid = randomUUID();

    active.add(activeActivation);
    active.add(committed(uuid, change("a", "100MB"), 2L));

    passive.add(passiveActivation);
    passive.add(committed(uuid, change("a", "100MB"), 2L));
    passive.add(prepared(randomUUID(), change("b", "200MB"), 3L)); // partial prepare

    mockDiscoveryForRepair();

    assertThat(sync(), hasItem(NOTHING));

    check(0, 0, 1);
  }

  @Test
  public void test_11_partial_prepare_on_active() throws NomadException {
    UUID uuid = randomUUID();

    active.add(activeActivation);
    active.add(committed(uuid, change("a", "100MB"), 2L));
    active.add(prepared(randomUUID(), change("b", "200MB"), 3L)); // partial prepare

    passive.add(passiveActivation);
    passive.add(committed(uuid, change("a", "100MB"), 2L));

    assertThat(sync(), hasItem(NOTHING));

    check(1, 0, 0);
  }

  @Test
  public void test_12_new_commits() throws NomadException {
    UUID uuid = randomUUID();

    active.add(activeActivation);
    active.add(committed(uuid, change("a", "100MB"), 2L));
    active.add(committed(randomUUID(), change("b", "200MB"), 3L));

    passive.add(passiveActivation);
    passive.add(committed(uuid, change("a", "100MB"), 2L));

    assertThat(sync(), hasItem(RESTART_REQUIRED));

    check(1, 1, 0);
  }

  @Test
  public void test_13_missing_rollback_on_active() throws NomadException {
    active.add(activeActivation);
    active.add(rolledBack(randomUUID(), change("a", "100MB"), 2L));

    passive.add(passiveActivation);

    assertThat(sync(), hasItem(NOTHING));

    check(0, 0, 0);
  }

  @Test
  public void test_14_missing_rollback_on_passive() throws NomadException {
    active.add(activeActivation);

    passive.add(passiveActivation);
    passive.add(rolledBack(randomUUID(), change("a", "100MB"), 2L));

    assertThat(sync(), hasItem(NOTHING));

    check(0, 0, 0);
  }

  @Test
  public void test_sync_restricted_activation() throws NomadException {
    active.add(activeActivation);
    active.add(committed(randomUUID(), change("a", "100MB"), 2L));
    activeTopology.getCluster().putOffheapResource("a", 100, MB);

    // disjoint activation
    passiveActivation = committed(randomUUID(), new ClusterActivationNomadChange(activeTopology.getCluster().clone()), 1L);
    passive.add(passiveActivation);

    sync();

    verify(passiveNomadServer).forceSync(any(), any());

    check(0, 0, 0);
  }

  @Test
  public void test_sync_restricted_activation_wrong_topology() throws NomadException {
    active.add(activeActivation);
    active.add(committed(randomUUID(), change("a", "100MB"), 2L));
    activeTopology.getCluster().putOffheapResource("a", 100, MB);

    // disjoint activation
    passiveActivation = committed(randomUUID(), new ClusterActivationNomadChange(activeTopology.getCluster().clone().putOffheapResource("a", 200, MB)), 1L);
    passive.add(passiveActivation);

    assertThat(this::sync,
        throwing(instanceOf(IllegalStateException.class)).andMessage(startsWith("Unable to find any change in the source node matching the topology used to activate this node.")));
  }

  private void check(int prepare, int commits, int rollbacks) throws NomadException {
    verify(passiveNomadServer, times(prepare)).prepare(any());
    verify(passiveNomadServer, times(commits)).commit(any());
    verify(passiveNomadServer, times(rollbacks)).rollback(any());
  }

  @SuppressWarnings("unchecked")
  private void mockDiscoveryForRepair() throws NomadException {
    DiscoverResponse<NodeContext> discoverResponse = mock(DiscoverResponse.class);
    ChangeDetails<NodeContext> changeDetails = mock(ChangeDetails.class);
    when(passiveNomadServer.discover()).thenReturn(discoverResponse);
    when(discoverResponse.getLatestChange()).thenReturn(changeDetails);
    when(changeDetails.getState()).thenReturn(PREPARED);
  }

  private Set<Require> sync() throws NomadException {
    return passiveSyncManager.sync(codec.decode(codec.encode(activeSyncManager.getSyncData())));
  }

  private NomadChangeInfo prepared(UUID changeUuid, NomadChange nomadChange, long version) {
    return prepared(changeUuid, nomadChange, version, changeUuid.toString());
  }

  private NomadChangeInfo prepared(UUID changeUuid, NomadChange nomadChange, long version, String changeResultHash) {
    return new NomadChangeInfo(changeUuid, nomadChange, PREPARED, version, "SYSTEM", "SYSTEM", now, changeResultHash);
  }

  private NomadChangeInfo committed(UUID changeUuid, NomadChange nomadChange, long version) {
    return committed(changeUuid, nomadChange, version, changeUuid.toString());
  }

  private NomadChangeInfo committed(UUID changeUuid, NomadChange nomadChange, long version, String changeResultHash) {
    return new NomadChangeInfo(changeUuid, nomadChange, COMMITTED, version, "SYSTEM", "SYSTEM", now, changeResultHash);
  }

  private NomadChangeInfo rolledBack(UUID changeUuid, NomadChange nomadChange, long version) {
    return rolledBack(changeUuid, nomadChange, version, changeUuid.toString());
  }

  private NomadChangeInfo rolledBack(UUID changeUuid, NomadChange nomadChange, long version, String changeResultHash) {
    return new NomadChangeInfo(changeUuid, nomadChange, ROLLED_BACK, version, "SYSTEM", "SYSTEM", now, changeResultHash);
  }

  private static SettingNomadChange change(String resourceName, String size) {
    return SettingNomadChange.set(cluster(), Setting.OFFHEAP_RESOURCES, resourceName, size);
  }
}