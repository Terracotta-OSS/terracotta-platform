/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.server.config_provider;

import com.tc.exception.TCServerRestartException;
import com.tc.exception.TCShutdownServerException;
import com.terracottatech.dynamic_config.api.model.NodeContext;
import com.terracottatech.dynamic_config.api.model.Setting;
import com.terracottatech.dynamic_config.api.model.nomad.SettingNomadChange;
import com.terracottatech.nomad.messages.AcceptRejectResponse;
import com.terracottatech.nomad.messages.CommitMessage;
import com.terracottatech.nomad.messages.DiscoverResponse;
import com.terracottatech.nomad.messages.PrepareMessage;
import com.terracottatech.nomad.server.ChangeRequestState;
import com.terracottatech.nomad.server.NomadChangeInfo;
import com.terracottatech.nomad.server.NomadException;
import com.terracottatech.nomad.server.UpgradableNomadServer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.terracottatech.dynamic_config.api.model.nomad.Applicability.cluster;
import static com.terracottatech.dynamic_config.server.config_provider.ConfigurationSyncManager.Codec.decode;
import static com.terracottatech.dynamic_config.server.config_provider.ConfigurationSyncManager.Codec.encode;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConfigurationSyncManagerTest {
  @Rule
  public ExpectedException exceptionRule = ExpectedException.none();
  private Instant now = Instant.now();

  @Test
  public void testCodec() {
    List<NomadChangeInfo> nomadChanges = new ArrayList<>();
    nomadChanges.add(new NomadChangeInfo(UUID.randomUUID(), createOffheapChange("a", "100"), ChangeRequestState.COMMITTED, 1L, "SYSTEM", "SYSTEM", now));
    nomadChanges.add(new NomadChangeInfo(UUID.randomUUID(), createOffheapChange("b", "200"), ChangeRequestState.COMMITTED, 1L, "SYSTEM", "SYSTEM", now));

    List<NomadChangeInfo> decodedChanges = decode(encode(nomadChanges));
    System.out.println(new String(encode(nomadChanges)));
    assertThat(decodedChanges, is(nomadChanges));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testSyncWhenPassiveHasMoreChanges() throws NomadException {
    List<NomadChangeInfo> activeNomadChanges = new ArrayList<>();
    UUID firstChange = UUID.randomUUID();
    activeNomadChanges.add(new NomadChangeInfo(firstChange, createOffheapChange("a", "100"), ChangeRequestState.COMMITTED, 1L, "SYSTEM", "SYSTEM", now));

    UpgradableNomadServer<NodeContext> activeNomadServer = mock(UpgradableNomadServer.class);
    ConfigurationSyncManager activeSyncManager = new ConfigurationSyncManager(activeNomadServer);
    when(activeNomadServer.getAllNomadChanges()).thenReturn(activeNomadChanges);
    byte[] active = activeSyncManager.getSyncData();

    List<NomadChangeInfo> passiveNomadChanges = new ArrayList<>();
    passiveNomadChanges.add(new NomadChangeInfo(firstChange, createOffheapChange("a", "100"), ChangeRequestState.COMMITTED, 1L, "SYSTEM", "SYSTEM", now));
    passiveNomadChanges.add(new NomadChangeInfo(UUID.randomUUID(), createOffheapChange("b", "200"), ChangeRequestState.COMMITTED, 2L, "SYSTEM", "SYSTEM", now));

    UpgradableNomadServer<NodeContext> nomadServer = mock(UpgradableNomadServer.class);
    when(nomadServer.getAllNomadChanges()).thenReturn(passiveNomadChanges);

    ConfigurationSyncManager syncManager = new ConfigurationSyncManager(nomadServer);
    exceptionRule.expect(TCShutdownServerException.class);
    exceptionRule.expectMessage("Passive has more configuration changes");
    syncManager.sync(active);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testSyncWhenPassiveChangeHistoryNotMatchWithActive() throws NomadException {
    List<NomadChangeInfo> activeNomadChanges = new ArrayList<>();
    activeNomadChanges.add(new NomadChangeInfo(UUID.randomUUID(), createOffheapChange("a", "100"), ChangeRequestState.COMMITTED, 1L, "SYSTEM", "SYSTEM", now));
    activeNomadChanges.add(new NomadChangeInfo(UUID.randomUUID(), createOffheapChange("b", "200"), ChangeRequestState.COMMITTED, 2L, "SYSTEM", "SYSTEM", now));

    UpgradableNomadServer<NodeContext> activeNomadServer = mock(UpgradableNomadServer.class);
    ConfigurationSyncManager activeSyncManager = new ConfigurationSyncManager(activeNomadServer);
    when(activeNomadServer.getAllNomadChanges()).thenReturn(activeNomadChanges);
    byte[] active = activeSyncManager.getSyncData();

    List<NomadChangeInfo> passiveNomadChanges = new ArrayList<>();
    passiveNomadChanges.add(new NomadChangeInfo(UUID.randomUUID(), createOffheapChange("a", "100"), ChangeRequestState.COMMITTED, 1L, "SYSTEM", "SYSTEM", now));

    UpgradableNomadServer<NodeContext> nomadServer = mock(UpgradableNomadServer.class);
    when(nomadServer.getAllNomadChanges()).thenReturn(passiveNomadChanges);

    ConfigurationSyncManager syncManager = new ConfigurationSyncManager(nomadServer);
    exceptionRule.expect(TCShutdownServerException.class);
    exceptionRule.expectMessage("Passive cannot sync because the configuration change history does not match");
    syncManager.sync(active);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testSyncWhenActiveHasChangesWhichIsNotCommitted() throws NomadException {
    List<NomadChangeInfo> activeNomadChanges = new ArrayList<>();
    UUID firstChange = UUID.randomUUID();
    activeNomadChanges.add(new NomadChangeInfo(firstChange, createOffheapChange("a", "100"), ChangeRequestState.COMMITTED, 1L, "SYSTEM", "SYSTEM", now));
    activeNomadChanges.add(new NomadChangeInfo(UUID.randomUUID(), createOffheapChange("b", "200"), ChangeRequestState.PREPARED, 2L, "SYSTEM", "SYSTEM", now));

    UpgradableNomadServer<NodeContext> activeNomadServer = mock(UpgradableNomadServer.class);
    ConfigurationSyncManager activeSyncManager = new ConfigurationSyncManager(activeNomadServer);
    when(activeNomadServer.getAllNomadChanges()).thenReturn(activeNomadChanges);
    byte[] active = activeSyncManager.getSyncData();

    List<NomadChangeInfo> passiveNomadChanges = new ArrayList<>();
    passiveNomadChanges.add(new NomadChangeInfo(firstChange, createOffheapChange("a", "100"), ChangeRequestState.COMMITTED, 1L, "SYSTEM", "SYSTEM", now));

    UpgradableNomadServer<NodeContext> nomadServer = mock(UpgradableNomadServer.class);
    DiscoverResponse<NodeContext> discoverResponse = mock(DiscoverResponse.class);
    when(nomadServer.getAllNomadChanges()).thenReturn(passiveNomadChanges);
    when(nomadServer.discover()).thenReturn(discoverResponse);

    ConfigurationSyncManager syncManager = new ConfigurationSyncManager(nomadServer);
    exceptionRule.expect(TCShutdownServerException.class);
    exceptionRule.expectMessage("Active has some PREPARED configuration changes that are not yet committed.");
    syncManager.sync(active);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testForRestartWhenPassiveSyncDataFromActive() throws NomadException {
    List<NomadChangeInfo> activeNomadChanges = new ArrayList<>();
    UUID firstChange = UUID.randomUUID();
    activeNomadChanges.add(new NomadChangeInfo(firstChange, createOffheapChange("a", "100"), ChangeRequestState.COMMITTED, 1L, "SYSTEM", "SYSTEM", now));
    activeNomadChanges.add(new NomadChangeInfo(UUID.randomUUID(), createOffheapChange("b", "200"), ChangeRequestState.COMMITTED, 2L, "SYSTEM", "SYSTEM", now));

    UpgradableNomadServer<NodeContext> activeNomadServer = mock(UpgradableNomadServer.class);
    ConfigurationSyncManager activeSyncManager = new ConfigurationSyncManager(activeNomadServer);
    when(activeNomadServer.getAllNomadChanges()).thenReturn(activeNomadChanges);
    byte[] active = activeSyncManager.getSyncData();

    List<NomadChangeInfo> passiveNomadChanges = new ArrayList<>();
    passiveNomadChanges.add(new NomadChangeInfo(firstChange, createOffheapChange("a", "100"), ChangeRequestState.COMMITTED, 1L, "SYSTEM", "SYSTEM", now));

    UpgradableNomadServer<NodeContext> nomadServer = mock(UpgradableNomadServer.class);
    DiscoverResponse<NodeContext> discoverResponse = mock(DiscoverResponse.class);
    AcceptRejectResponse acceptRejectResponse = mock(AcceptRejectResponse.class);
    when(nomadServer.getAllNomadChanges()).thenReturn(passiveNomadChanges);
    when(nomadServer.discover()).thenReturn(discoverResponse);
    when(nomadServer.prepare(any(PrepareMessage.class))).thenReturn(acceptRejectResponse);
    when(nomadServer.commit(any(CommitMessage.class))).thenReturn(acceptRejectResponse);
    when(acceptRejectResponse.isAccepted()).thenReturn(true);

    ConfigurationSyncManager syncManager = new ConfigurationSyncManager(nomadServer);
    exceptionRule.expect(TCServerRestartException.class);
    exceptionRule.expectMessage("Restarting server");
    syncManager.sync(active);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testNoRestartInPassiveBecauseOfSameChangeAsActive() throws NomadException {
    List<NomadChangeInfo> activeNomadChanges = new ArrayList<>();
    UUID firstChange = UUID.randomUUID();
    UUID secondChange = UUID.randomUUID();
    activeNomadChanges.add(new NomadChangeInfo(firstChange, createOffheapChange("a", "100"), ChangeRequestState.COMMITTED, 1L, "SYSTEM", "SYSTEM", now));
    activeNomadChanges.add(new NomadChangeInfo(secondChange, createOffheapChange("b", "200"), ChangeRequestState.COMMITTED, 2L, "SYSTEM", "SYSTEM", now));

    UpgradableNomadServer<NodeContext> activeNomadServer = mock(UpgradableNomadServer.class);
    ConfigurationSyncManager activeSyncManager = new ConfigurationSyncManager(activeNomadServer);
    when(activeNomadServer.getAllNomadChanges()).thenReturn(activeNomadChanges);
    byte[] active = activeSyncManager.getSyncData();

    List<NomadChangeInfo> passiveNomadChanges = new ArrayList<>();
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

    ConfigurationSyncManager syncManager = new ConfigurationSyncManager(nomadServer);
    syncManager.sync(active);
  }

  private static SettingNomadChange createOffheapChange(String resourceName, String size) {
    return SettingNomadChange.set(cluster(), Setting.OFFHEAP_RESOURCES, resourceName, size);
  }
}