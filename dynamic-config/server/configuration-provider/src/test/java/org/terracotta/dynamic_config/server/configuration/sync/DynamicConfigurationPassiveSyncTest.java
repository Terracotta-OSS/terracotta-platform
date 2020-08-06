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
package org.terracotta.dynamic_config.server.configuration.sync;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.terracotta.dynamic_config.api.json.DynamicConfigApiJsonModule;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.Setting;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.Testing;
import org.terracotta.dynamic_config.api.model.nomad.ClusterActivationNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.SettingNomadChange;
import org.terracotta.dynamic_config.api.service.DynamicConfigService;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.json.ObjectMapperFactory;
import org.terracotta.nomad.messages.AcceptRejectResponse;
import org.terracotta.nomad.messages.CommitMessage;
import org.terracotta.nomad.messages.DiscoverResponse;
import org.terracotta.nomad.messages.PrepareMessage;
import org.terracotta.nomad.server.ChangeRequestState;
import org.terracotta.nomad.server.NomadChangeInfo;
import org.terracotta.nomad.server.NomadException;
import org.terracotta.nomad.server.UpgradableNomadServer;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.terracotta.dynamic_config.api.model.nomad.Applicability.cluster;
import static org.terracotta.dynamic_config.server.configuration.sync.Require.CAN_CONTINUE;
import static org.terracotta.dynamic_config.server.configuration.sync.Require.RESTART_REQUIRED;

public class DynamicConfigurationPassiveSyncTest {
  @Rule
  public ExpectedException exceptionRule = ExpectedException.none();
  private Instant now = Instant.now();
  private NodeContext startupTopology = new NodeContext(Testing.newTestCluster("foo", new Stripe(Testing.newTestNode("bar", "localhost"))), 1, "bar");
  private NomadChangeInfo activation = new NomadChangeInfo(UUID.randomUUID(), new ClusterActivationNomadChange(startupTopology.getCluster()), ChangeRequestState.COMMITTED, 1L, "SYSTEM", "SYSTEM", now);
  private DynamicConfigSyncData.Codec codec = new DynamicConfigSyncData.Codec(new ObjectMapperFactory().withModule(new DynamicConfigApiJsonModule()));
  private TopologyService topologyService;

  @Before
  public void setUp() throws Exception {
    topologyService = mock(TopologyService.class);
    when(topologyService.getUpcomingNodeContext()).thenReturn(startupTopology);
  }

  @Test
  public void testCodec() {
    List<NomadChangeInfo> nomadChanges = new ArrayList<>();
    nomadChanges.add(new NomadChangeInfo(UUID.randomUUID(), createOffheapChange("a", "100"), ChangeRequestState.COMMITTED, 1L, "SYSTEM", "SYSTEM", now));
    nomadChanges.add(new NomadChangeInfo(UUID.randomUUID(), createOffheapChange("b", "200"), ChangeRequestState.COMMITTED, 1L, "SYSTEM", "SYSTEM", now));

    List<NomadChangeInfo> decodedChanges = codec.decode(codec.encode(new DynamicConfigSyncData(nomadChanges, startupTopology.getCluster(), null))).getNomadChanges();
    assertThat(decodedChanges, is(nomadChanges));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testSyncWhenPassiveHasMoreChanges() throws NomadException {
    List<NomadChangeInfo> activeNomadChanges = new ArrayList<>();
    UUID firstChange = UUID.randomUUID();
    activeNomadChanges.add(activation);
    activeNomadChanges.add(new NomadChangeInfo(firstChange, createOffheapChange("a", "100"), ChangeRequestState.COMMITTED, 1L, "SYSTEM", "SYSTEM", now));

    UpgradableNomadServer<NodeContext> activeNomadServer = mock(UpgradableNomadServer.class);
    DynamicConfigurationPassiveSync activeSyncManager = new DynamicConfigurationPassiveSync(startupTopology, activeNomadServer, mock(DynamicConfigService.class), topologyService, () -> null);
    when(activeNomadServer.getAllNomadChanges()).thenReturn(activeNomadChanges);
    byte[] active = codec.encode(activeSyncManager.getSyncData());

    List<NomadChangeInfo> passiveNomadChanges = new ArrayList<>();
    passiveNomadChanges.add(activation);
    passiveNomadChanges.add(new NomadChangeInfo(firstChange, createOffheapChange("a", "100"), ChangeRequestState.COMMITTED, 1L, "SYSTEM", "SYSTEM", now));
    passiveNomadChanges.add(new NomadChangeInfo(UUID.randomUUID(), createOffheapChange("b", "200"), ChangeRequestState.COMMITTED, 2L, "SYSTEM", "SYSTEM", now));

    UpgradableNomadServer<NodeContext> nomadServer = mock(UpgradableNomadServer.class);
    when(nomadServer.getAllNomadChanges()).thenReturn(passiveNomadChanges);

    DynamicConfigurationPassiveSync syncManager = new DynamicConfigurationPassiveSync(startupTopology, nomadServer, mock(DynamicConfigService.class), topologyService, () -> null);
    exceptionRule.expect(IllegalStateException.class);
    exceptionRule.expectMessage("Node has more configuration changes");
    syncManager.sync(codec.decode(active));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testSyncLicense() throws NomadException {
    List<NomadChangeInfo> sameChanges = new ArrayList<>();
    sameChanges.add(activation);
    sameChanges.add(new NomadChangeInfo(UUID.randomUUID(), createOffheapChange("a", "100"), ChangeRequestState.COMMITTED, 1L, "SYSTEM", "SYSTEM", now));

    UpgradableNomadServer<NodeContext> activeNomadServer = mock(UpgradableNomadServer.class);
    DynamicConfigurationPassiveSync activeSyncManager = new DynamicConfigurationPassiveSync(startupTopology, activeNomadServer, mock(DynamicConfigService.class), topologyService, () -> "license content");
    when(activeNomadServer.getAllNomadChanges()).thenReturn(sameChanges);
    byte[] active = codec.encode(activeSyncManager.getSyncData());

    UpgradableNomadServer<NodeContext> nomadServer = mock(UpgradableNomadServer.class);
    when(nomadServer.getAllNomadChanges()).thenReturn(sameChanges);

    DynamicConfigService dynamicConfigService = mock(DynamicConfigService.class);
    DynamicConfigurationPassiveSync syncManager = new DynamicConfigurationPassiveSync(startupTopology, nomadServer, dynamicConfigService, topologyService, () -> null);

    Set<Require> sync = syncManager.sync(codec.decode(active));
    assertThat(sync.size(), is(equalTo(0)));

    verify(dynamicConfigService).upgradeLicense("license content");
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testSyncWhenPassiveChangeHistoryNotMatchWithActive() throws NomadException {
    List<NomadChangeInfo> activeNomadChanges = new ArrayList<>();
    activeNomadChanges.add(activation);
    activeNomadChanges.add(new NomadChangeInfo(UUID.randomUUID(), createOffheapChange("a", "100"), ChangeRequestState.COMMITTED, 1L, "SYSTEM", "SYSTEM", now));
    activeNomadChanges.add(new NomadChangeInfo(UUID.randomUUID(), createOffheapChange("b", "200"), ChangeRequestState.COMMITTED, 2L, "SYSTEM", "SYSTEM", now));

    UpgradableNomadServer<NodeContext> activeNomadServer = mock(UpgradableNomadServer.class);
    DynamicConfigurationPassiveSync activeSyncManager = new DynamicConfigurationPassiveSync(startupTopology, activeNomadServer, mock(DynamicConfigService.class), topologyService, () -> null);
    when(activeNomadServer.getAllNomadChanges()).thenReturn(activeNomadChanges);
    byte[] active = codec.encode(activeSyncManager.getSyncData());

    List<NomadChangeInfo> passiveNomadChanges = new ArrayList<>();
    passiveNomadChanges.add(activation);
    passiveNomadChanges.add(new NomadChangeInfo(UUID.randomUUID(), createOffheapChange("a", "100"), ChangeRequestState.COMMITTED, 1L, "SYSTEM", "SYSTEM", now));

    UpgradableNomadServer<NodeContext> nomadServer = mock(UpgradableNomadServer.class);
    when(nomadServer.getAllNomadChanges()).thenReturn(passiveNomadChanges);

    DynamicConfigurationPassiveSync syncManager = new DynamicConfigurationPassiveSync(startupTopology, nomadServer, mock(DynamicConfigService.class), topologyService, () -> null);
    exceptionRule.expect(IllegalStateException.class);
    exceptionRule.expectMessage("Node cannot sync because the configuration change history does not match");
    syncManager.sync(codec.decode(active));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testSyncWhenActiveHasChangesWhichIsNotCommitted() throws NomadException {
    List<NomadChangeInfo> activeNomadChanges = new ArrayList<>();
    UUID firstChange = UUID.randomUUID();
    activeNomadChanges.add(activation);
    activeNomadChanges.add(new NomadChangeInfo(firstChange, createOffheapChange("a", "100"), ChangeRequestState.COMMITTED, 1L, "SYSTEM", "SYSTEM", now));
    activeNomadChanges.add(new NomadChangeInfo(UUID.randomUUID(), createOffheapChange("b", "200"), ChangeRequestState.PREPARED, 2L, "SYSTEM", "SYSTEM", now));

    UpgradableNomadServer<NodeContext> activeNomadServer = mock(UpgradableNomadServer.class);
    DynamicConfigurationPassiveSync activeSyncManager = new DynamicConfigurationPassiveSync(startupTopology, activeNomadServer, mock(DynamicConfigService.class), topologyService, () -> null);
    when(activeNomadServer.getAllNomadChanges()).thenReturn(activeNomadChanges);
    byte[] active = codec.encode(activeSyncManager.getSyncData());

    List<NomadChangeInfo> passiveNomadChanges = new ArrayList<>();
    passiveNomadChanges.add(activation);
    passiveNomadChanges.add(new NomadChangeInfo(firstChange, createOffheapChange("a", "100"), ChangeRequestState.COMMITTED, 1L, "SYSTEM", "SYSTEM", now));

    UpgradableNomadServer<NodeContext> nomadServer = mock(UpgradableNomadServer.class);
    DiscoverResponse<NodeContext> discoverResponse = mock(DiscoverResponse.class);
    AcceptRejectResponse acceptRejectResponse = mock(AcceptRejectResponse.class);
    when(nomadServer.getAllNomadChanges()).thenReturn(passiveNomadChanges);
    when(nomadServer.discover()).thenReturn(discoverResponse);
    when(nomadServer.prepare(any(PrepareMessage.class))).thenReturn(acceptRejectResponse);
    when(acceptRejectResponse.isAccepted()).thenReturn(true);

    DynamicConfigurationPassiveSync syncManager = new DynamicConfigurationPassiveSync(startupTopology, nomadServer, mock(DynamicConfigService.class), topologyService, () -> null);
    Set<Require> requires = syncManager.sync(codec.decode(active));
    assertThat(requires.size(), is(equalTo(1)));
    assertThat(requires, hasItem(CAN_CONTINUE));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testForRestartWhenPassiveSyncDataFromActive() throws NomadException {
    List<NomadChangeInfo> activeNomadChanges = new ArrayList<>();
    UUID firstChange = UUID.randomUUID();
    activeNomadChanges.add(activation);
    activeNomadChanges.add(new NomadChangeInfo(firstChange, createOffheapChange("a", "100"), ChangeRequestState.COMMITTED, 1L, "SYSTEM", "SYSTEM", now));
    activeNomadChanges.add(new NomadChangeInfo(UUID.randomUUID(), createOffheapChange("b", "200"), ChangeRequestState.COMMITTED, 2L, "SYSTEM", "SYSTEM", now));

    UpgradableNomadServer<NodeContext> activeNomadServer = mock(UpgradableNomadServer.class);
    DynamicConfigurationPassiveSync activeSyncManager = new DynamicConfigurationPassiveSync(startupTopology, activeNomadServer, mock(DynamicConfigService.class), topologyService, () -> null);
    when(activeNomadServer.getAllNomadChanges()).thenReturn(activeNomadChanges);
    byte[] active = codec.encode(activeSyncManager.getSyncData());

    List<NomadChangeInfo> passiveNomadChanges = new ArrayList<>();
    passiveNomadChanges.add(activation);
    passiveNomadChanges.add(new NomadChangeInfo(firstChange, createOffheapChange("a", "100"), ChangeRequestState.COMMITTED, 1L, "SYSTEM", "SYSTEM", now));

    UpgradableNomadServer<NodeContext> nomadServer = mock(UpgradableNomadServer.class);
    DiscoverResponse<NodeContext> discoverResponse = mock(DiscoverResponse.class);
    AcceptRejectResponse acceptRejectResponse = mock(AcceptRejectResponse.class);
    when(nomadServer.getAllNomadChanges()).thenReturn(passiveNomadChanges);
    when(nomadServer.discover()).thenReturn(discoverResponse);
    when(nomadServer.prepare(any(PrepareMessage.class))).thenReturn(acceptRejectResponse);
    when(nomadServer.commit(any(CommitMessage.class))).thenReturn(acceptRejectResponse);
    when(acceptRejectResponse.isAccepted()).thenReturn(true);

    DynamicConfigurationPassiveSync syncManager = new DynamicConfigurationPassiveSync(startupTopology, nomadServer, mock(DynamicConfigService.class), topologyService, () -> null);
    Set<Require> requires = syncManager.sync(codec.decode(active));
    assertThat(requires.size(), is(equalTo(1)));
    assertThat(requires, hasItem(RESTART_REQUIRED));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testNoRestartInPassiveBecauseOfSameChangeAsActive() throws NomadException {
    List<NomadChangeInfo> activeNomadChanges = new ArrayList<>();
    UUID firstChange = UUID.randomUUID();
    UUID secondChange = UUID.randomUUID();
    activeNomadChanges.add(activation);
    activeNomadChanges.add(new NomadChangeInfo(firstChange, createOffheapChange("a", "100"), ChangeRequestState.COMMITTED, 1L, "SYSTEM", "SYSTEM", now));
    activeNomadChanges.add(new NomadChangeInfo(secondChange, createOffheapChange("b", "200"), ChangeRequestState.COMMITTED, 2L, "SYSTEM", "SYSTEM", now));

    UpgradableNomadServer<NodeContext> activeNomadServer = mock(UpgradableNomadServer.class);
    DynamicConfigurationPassiveSync activeSyncManager = new DynamicConfigurationPassiveSync(startupTopology, activeNomadServer, mock(DynamicConfigService.class), topologyService, () -> null);
    when(activeNomadServer.getAllNomadChanges()).thenReturn(activeNomadChanges);
    byte[] active = codec.encode(activeSyncManager.getSyncData());

    List<NomadChangeInfo> passiveNomadChanges = new ArrayList<>();
    passiveNomadChanges.add(activation);
    passiveNomadChanges.add(new NomadChangeInfo(firstChange, createOffheapChange("a", "100"), ChangeRequestState.COMMITTED, 1L, "SYSTEM", "SYSTEM", now));
    passiveNomadChanges.add(new NomadChangeInfo(secondChange, createOffheapChange("b", "200"), ChangeRequestState.COMMITTED, 2L, "SYSTEM", "SYSTEM", now));
    UpgradableNomadServer<NodeContext> nomadServer = mock(UpgradableNomadServer.class);
    DiscoverResponse<NodeContext> discoverResponse = mock(DiscoverResponse.class);
    AcceptRejectResponse acceptRejectResponse = mock(AcceptRejectResponse.class);
    when(nomadServer.getAllNomadChanges()).thenReturn(passiveNomadChanges);
    when(nomadServer.discover()).thenReturn(discoverResponse);
    when(nomadServer.prepare(any(PrepareMessage.class))).thenReturn(acceptRejectResponse);
    when(nomadServer.commit(any(CommitMessage.class))).thenReturn(acceptRejectResponse);
    when(acceptRejectResponse.isAccepted()).thenReturn(true);

    DynamicConfigurationPassiveSync syncManager = new DynamicConfigurationPassiveSync(startupTopology, nomadServer, mock(DynamicConfigService.class), topologyService, () -> null);
    Set<Require> requires = syncManager.sync(codec.decode(active));
    assertThat(requires.size(), is(equalTo(0)));
  }

  private static SettingNomadChange createOffheapChange(String resourceName, String size) {
    return SettingNomadChange.set(cluster(), Setting.OFFHEAP_RESOURCES, resourceName, size);
  }
}