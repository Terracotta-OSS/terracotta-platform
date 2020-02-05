/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.server.sync;

import com.tc.exception.TCServerRestartException;
import com.tc.exception.TCShutdownServerException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.Setting;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.nomad.SettingNomadChange;
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
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.terracotta.dynamic_config.api.model.nomad.Applicability.cluster;
import static org.terracotta.dynamic_config.server.sync.DynamicConfigurationPassiveSync.Codec.decode;
import static org.terracotta.dynamic_config.server.sync.DynamicConfigurationPassiveSync.Codec.encode;

public class DynamicConfigurationPassiveSyncTest {
  @Rule
  public ExpectedException exceptionRule = ExpectedException.none();
  private Instant now = Instant.now();

  private NodeContext nodeContext = new NodeContext(new Cluster("foo", new Stripe(Node.newDefaultNode("node1", "localhost"))), 1, "node1");

  @Test
  public void testCodec() {
    List<NomadChangeInfo<NodeContext>> nomadChanges = new ArrayList<>();
    nomadChanges.add(new NomadChangeInfo<>(UUID.randomUUID(), createOffheapChange("a", "100"), ChangeRequestState.COMMITTED, 1L, "SYSTEM", "SYSTEM", now, nodeContext));
    nomadChanges.add(new NomadChangeInfo<>(UUID.randomUUID(), createOffheapChange("b", "200"), ChangeRequestState.COMMITTED, 1L, "SYSTEM", "SYSTEM", now, nodeContext));

    List<NomadChangeInfo<NodeContext>> decodedChanges = decode(encode(nomadChanges));
    assertThat(decodedChanges, is(nomadChanges));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testSyncWhenPassiveHasMoreChanges() throws NomadException {
    List<NomadChangeInfo<NodeContext>> activeNomadChanges = new ArrayList<>();
    UUID firstChange = UUID.randomUUID();
    activeNomadChanges.add(new NomadChangeInfo<>(firstChange, createOffheapChange("a", "100"), ChangeRequestState.COMMITTED, 1L, "SYSTEM", "SYSTEM", now, nodeContext));

    UpgradableNomadServer<NodeContext> activeNomadServer = mock(UpgradableNomadServer.class);
    DynamicConfigurationPassiveSync activeSyncManager = new DynamicConfigurationPassiveSync(activeNomadServer);
    when(activeNomadServer.getAllNomadChanges()).thenReturn(activeNomadChanges);
    byte[] active = activeSyncManager.getSyncData();

    List<NomadChangeInfo<NodeContext>> passiveNomadChanges = new ArrayList<>();
    passiveNomadChanges.add(new NomadChangeInfo<>(firstChange, createOffheapChange("a", "100"), ChangeRequestState.COMMITTED, 1L, "SYSTEM", "SYSTEM", now, nodeContext));
    passiveNomadChanges.add(new NomadChangeInfo<>(UUID.randomUUID(), createOffheapChange("b", "200"), ChangeRequestState.COMMITTED, 2L, "SYSTEM", "SYSTEM", now, nodeContext));

    UpgradableNomadServer<NodeContext> nomadServer = mock(UpgradableNomadServer.class);
    when(nomadServer.getAllNomadChanges()).thenReturn(passiveNomadChanges);

    DynamicConfigurationPassiveSync syncManager = new DynamicConfigurationPassiveSync(nomadServer);
    exceptionRule.expect(TCShutdownServerException.class);
    exceptionRule.expectMessage("Passive has more configuration changes");
    syncManager.sync(active);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testSyncWhenPassiveChangeHistoryNotMatchWithActive() throws NomadException {
    List<NomadChangeInfo<NodeContext>> activeNomadChanges = new ArrayList<>();
    activeNomadChanges.add(new NomadChangeInfo<>(UUID.randomUUID(), createOffheapChange("a", "100"), ChangeRequestState.COMMITTED, 1L, "SYSTEM", "SYSTEM", now, nodeContext));
    activeNomadChanges.add(new NomadChangeInfo<>(UUID.randomUUID(), createOffheapChange("b", "200"), ChangeRequestState.COMMITTED, 2L, "SYSTEM", "SYSTEM", now, nodeContext));

    UpgradableNomadServer<NodeContext> activeNomadServer = mock(UpgradableNomadServer.class);
    DynamicConfigurationPassiveSync activeSyncManager = new DynamicConfigurationPassiveSync(activeNomadServer);
    when(activeNomadServer.getAllNomadChanges()).thenReturn(activeNomadChanges);
    byte[] active = activeSyncManager.getSyncData();

    List<NomadChangeInfo<NodeContext>> passiveNomadChanges = new ArrayList<>();
    passiveNomadChanges.add(new NomadChangeInfo<>(UUID.randomUUID(), createOffheapChange("a", "100"), ChangeRequestState.COMMITTED, 1L, "SYSTEM", "SYSTEM", now, nodeContext));

    UpgradableNomadServer<NodeContext> nomadServer = mock(UpgradableNomadServer.class);
    when(nomadServer.getAllNomadChanges()).thenReturn(passiveNomadChanges);

    DynamicConfigurationPassiveSync syncManager = new DynamicConfigurationPassiveSync(nomadServer);
    exceptionRule.expect(TCShutdownServerException.class);
    exceptionRule.expectMessage("Passive cannot sync because the configuration change history does not match");
    syncManager.sync(active);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testSyncWhenActiveHasChangesWhichIsNotCommitted() throws NomadException {
    List<NomadChangeInfo<NodeContext>> activeNomadChanges = new ArrayList<>();
    UUID firstChange = UUID.randomUUID();
    activeNomadChanges.add(new NomadChangeInfo<>(firstChange, createOffheapChange("a", "100"), ChangeRequestState.COMMITTED, 1L, "SYSTEM", "SYSTEM", now, nodeContext));
    activeNomadChanges.add(new NomadChangeInfo<>(UUID.randomUUID(), createOffheapChange("b", "200"), ChangeRequestState.PREPARED, 2L, "SYSTEM", "SYSTEM", now, nodeContext));

    UpgradableNomadServer<NodeContext> activeNomadServer = mock(UpgradableNomadServer.class);
    DynamicConfigurationPassiveSync activeSyncManager = new DynamicConfigurationPassiveSync(activeNomadServer);
    when(activeNomadServer.getAllNomadChanges()).thenReturn(activeNomadChanges);
    byte[] active = activeSyncManager.getSyncData();

    List<NomadChangeInfo<NodeContext>> passiveNomadChanges = new ArrayList<>();
    passiveNomadChanges.add(new NomadChangeInfo<>(firstChange, createOffheapChange("a", "100"), ChangeRequestState.COMMITTED, 1L, "SYSTEM", "SYSTEM", now, nodeContext));

    UpgradableNomadServer<NodeContext> nomadServer = mock(UpgradableNomadServer.class);
    DiscoverResponse<NodeContext> discoverResponse = mock(DiscoverResponse.class);
    when(nomadServer.getAllNomadChanges()).thenReturn(passiveNomadChanges);
    when(nomadServer.discover()).thenReturn(discoverResponse);

    DynamicConfigurationPassiveSync syncManager = new DynamicConfigurationPassiveSync(nomadServer);
    exceptionRule.expect(TCShutdownServerException.class);
    exceptionRule.expectMessage("Active has some PREPARED configuration changes that are not yet committed.");
    syncManager.sync(active);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testForRestartWhenPassiveSyncDataFromActive() throws NomadException {
    List<NomadChangeInfo<NodeContext>> activeNomadChanges = new ArrayList<>();
    UUID firstChange = UUID.randomUUID();
    activeNomadChanges.add(new NomadChangeInfo<>(firstChange, createOffheapChange("a", "100"), ChangeRequestState.COMMITTED, 1L, "SYSTEM", "SYSTEM", now, nodeContext));
    activeNomadChanges.add(new NomadChangeInfo<>(UUID.randomUUID(), createOffheapChange("b", "200"), ChangeRequestState.COMMITTED, 2L, "SYSTEM", "SYSTEM", now, nodeContext));

    UpgradableNomadServer<NodeContext> activeNomadServer = mock(UpgradableNomadServer.class);
    DynamicConfigurationPassiveSync activeSyncManager = new DynamicConfigurationPassiveSync(activeNomadServer);
    when(activeNomadServer.getAllNomadChanges()).thenReturn(activeNomadChanges);
    byte[] active = activeSyncManager.getSyncData();

    List<NomadChangeInfo<NodeContext>> passiveNomadChanges = new ArrayList<>();
    passiveNomadChanges.add(new NomadChangeInfo<>(firstChange, createOffheapChange("a", "100"), ChangeRequestState.COMMITTED, 1L, "SYSTEM", "SYSTEM", now, nodeContext));

    UpgradableNomadServer<NodeContext> nomadServer = mock(UpgradableNomadServer.class);
    DiscoverResponse<NodeContext> discoverResponse = mock(DiscoverResponse.class);
    AcceptRejectResponse acceptRejectResponse = mock(AcceptRejectResponse.class);
    when(nomadServer.getAllNomadChanges()).thenReturn(passiveNomadChanges);
    when(nomadServer.discover()).thenReturn(discoverResponse);
    when(nomadServer.prepare(any(PrepareMessage.class))).thenReturn(acceptRejectResponse);
    when(nomadServer.commit(any(CommitMessage.class))).thenReturn(acceptRejectResponse);
    when(acceptRejectResponse.isAccepted()).thenReturn(true);

    DynamicConfigurationPassiveSync syncManager = new DynamicConfigurationPassiveSync(nomadServer);
    exceptionRule.expect(TCServerRestartException.class);
    exceptionRule.expectMessage("Restarting server");
    syncManager.sync(active);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testNoRestartInPassiveBecauseOfSameChangeAsActive() throws NomadException {
    List<NomadChangeInfo<NodeContext>> activeNomadChanges = new ArrayList<>();
    UUID firstChange = UUID.randomUUID();
    UUID secondChange = UUID.randomUUID();
    activeNomadChanges.add(new NomadChangeInfo<>(firstChange, createOffheapChange("a", "100"), ChangeRequestState.COMMITTED, 1L, "SYSTEM", "SYSTEM", now, nodeContext));
    activeNomadChanges.add(new NomadChangeInfo<>(secondChange, createOffheapChange("b", "200"), ChangeRequestState.COMMITTED, 2L, "SYSTEM", "SYSTEM", now, nodeContext));

    UpgradableNomadServer<NodeContext> activeNomadServer = mock(UpgradableNomadServer.class);
    DynamicConfigurationPassiveSync activeSyncManager = new DynamicConfigurationPassiveSync(activeNomadServer);
    when(activeNomadServer.getAllNomadChanges()).thenReturn(activeNomadChanges);
    byte[] active = activeSyncManager.getSyncData();

    List<NomadChangeInfo<NodeContext>> passiveNomadChanges = new ArrayList<>();
    passiveNomadChanges.add(new NomadChangeInfo<>(firstChange, createOffheapChange("a", "100"), ChangeRequestState.COMMITTED, 1L, "SYSTEM", "SYSTEM", now, nodeContext));
    passiveNomadChanges.add(new NomadChangeInfo<>(secondChange, createOffheapChange("b", "200"), ChangeRequestState.COMMITTED, 2L, "SYSTEM", "SYSTEM", now, nodeContext));
    UpgradableNomadServer<NodeContext> nomadServer = mock(UpgradableNomadServer.class);
    DiscoverResponse<NodeContext> discoverResponse = mock(DiscoverResponse.class);
    AcceptRejectResponse acceptRejectResponse = mock(AcceptRejectResponse.class);
    when(nomadServer.getAllNomadChanges()).thenReturn(passiveNomadChanges);
    when(nomadServer.discover()).thenReturn(discoverResponse);
    when(nomadServer.prepare(any(PrepareMessage.class))).thenReturn(acceptRejectResponse);
    when(nomadServer.commit(any(CommitMessage.class))).thenReturn(acceptRejectResponse);
    when(acceptRejectResponse.isAccepted()).thenReturn(true);

    DynamicConfigurationPassiveSync syncManager = new DynamicConfigurationPassiveSync(nomadServer);
    syncManager.sync(active);
  }

  private static SettingNomadChange createOffheapChange(String resourceName, String size) {
    return SettingNomadChange.set(cluster(), Setting.OFFHEAP_RESOURCES, resourceName, size);
  }
}