/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.nomad.persistence;

import com.terracottatech.dynamic_config.nomad.Applicability;
import com.terracottatech.dynamic_config.nomad.NomadJson;
import com.terracottatech.dynamic_config.nomad.SettingNomadChange;
import com.terracottatech.nomad.client.change.NomadChange;
import com.terracottatech.nomad.server.ChangeRequest;
import com.terracottatech.persistence.sanskrit.MutableSanskritObject;
import com.terracottatech.persistence.sanskrit.Sanskrit;
import com.terracottatech.persistence.sanskrit.SanskritObject;
import com.terracottatech.persistence.sanskrit.SanskritObjectImpl;
import com.terracottatech.persistence.sanskrit.change.SanskritChange;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.UUID;

import static com.tc.util.Assert.assertTrue;
import static com.terracottatech.nomad.server.ChangeRequestState.COMMITTED;
import static com.terracottatech.nomad.server.ChangeRequestState.ROLLED_BACK;
import static com.terracottatech.nomad.server.NomadServerMode.ACCEPTING;
import static com.terracottatech.nomad.server.NomadServerMode.PREPARED;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SanskritNomadServerStateTest {
  @Mock
  private Sanskrit sanskrit;

  @Mock
  private ConfigStorage configStorage;

  @Captor
  private ArgumentCaptor<SanskritChange> sanskritChangeCaptor;

  private SanskritNomadServerState state;

  @Before
  public void before() {
    when(sanskrit.newMutableSanskritObject()).thenReturn(new SanskritObjectImpl(NomadJson.buildObjectMapper()));
    state = new SanskritNomadServerState(sanskrit, configStorage);
  }

  @Test
  public void isInitializedFalse() {
    assertFalse(state.isInitialized());
  }

  @Test
  public void getMode() throws Exception {
    when(sanskrit.getString("mode")).thenReturn("ACCEPTING");
    assertEquals(ACCEPTING, state.getMode());
    assertTrue(state.isInitialized());
  }

  @Test
  public void getMutativeMessageCount() throws Exception {
    when(sanskrit.getLong("mutativeMessageCount")).thenReturn(1L);
    assertEquals(1L, state.getMutativeMessageCount());
  }

  @Test
  public void getLastMutationHost() throws Exception {
    when(sanskrit.getString("lastMutationHost")).thenReturn("host");
    assertEquals("host", state.getLastMutationHost());
  }

  @Test
  public void getLastMutationUser() throws Exception {
    when(sanskrit.getString("lastMutationUser")).thenReturn("user");
    assertEquals("user", state.getLastMutationUser());
  }

  @Test
  public void getLatestChangeUuid() throws Exception {
    UUID uuid = UUID.randomUUID();
    when(sanskrit.getString("latestChangeUuid")).thenReturn(uuid.toString());
    assertEquals(uuid, state.getLatestChangeUuid());
  }

  @Test
  public void getCurrentVersion() throws Exception {
    when(sanskrit.getLong("currentVersion")).thenReturn(1L);
    assertEquals(1L, state.getCurrentVersion());
  }

  @Test
  public void getHighestVersion() throws Exception {
    when(sanskrit.getLong("highestVersion")).thenReturn(1L);
    assertEquals(1L, state.getHighestVersion());
  }

  @Test
  public void getChangeRequest() throws Exception {
    UUID uuid = UUID.randomUUID();

    SettingNomadChange settingNomadChange = SettingNomadChange.set(Applicability.cluster(), "offheap-resources.primary-server-resource", "2GB");

    MutableSanskritObject changeObject = sanskrit.newMutableSanskritObject();
    changeObject.setString("state", "ROLLED_BACK");
    changeObject.setLong("version", 1L);
    changeObject.setExternal("operation", settingNomadChange);
    changeObject.setString("changeResultHash", "c2c9b194778150614a8a1b127842fc6f42b1a5f4");
    changeObject.setString("creationHost", "host");
    changeObject.setString("creationUser", "user");
    changeObject.setString("summary", "description");

    when(sanskrit.getObject(uuid.toString())).thenReturn(changeObject);
    when(configStorage.getConfig(1L)).thenReturn("config");

    ChangeRequest changeRequest = state.getChangeRequest(uuid);
    SettingNomadChange change = (SettingNomadChange) changeRequest.getChange();

    assertEquals(ROLLED_BACK, changeRequest.getState());
    assertEquals(1L, changeRequest.getVersion());
    assertEquals(settingNomadChange, change);
    assertEquals("config", changeRequest.getChangeResult());
    assertEquals("host", changeRequest.getCreationHost());
    assertEquals("user", changeRequest.getCreationUser());
    assertEquals("set offheap-resources.primary-server-resource=2GB", change.getSummary());
  }

  @Test
  public void makeFirstChange() throws Exception {
    runChangeTest(1L);
  }

  @Test
  public void makeSubsequentChange() throws Exception {
    when(sanskrit.getLong("mutativeMessageCount")).thenReturn(10L);
    runChangeTest(11L);
  }

  private void runChangeTest(long expectedMutativeMessageCount) throws Exception {
    UUID uuid = UUID.randomUUID();

    SettingNomadChange settingNomadChange = SettingNomadChange.set(Applicability.cluster(), "offheap-resources.primary-server-resource", "2GB");

    ChangeRequest changeRequest = new ChangeRequest(
        COMMITTED,
        4L,
        settingNomadChange,
        "config",
        "host1",
        "user1"
    );

    state.applyStateChange(state.newStateChange()
        .setMode(PREPARED)
        .setLatestChangeUuid(uuid)
        .setCurrentVersion(2L)
        .setHighestVersion(3L)
        .setLastMutationHost("host2")
        .setLastMutationUser("user2")
        .createChange(uuid, changeRequest)
    );

    verify(sanskrit).applyChange(sanskritChangeCaptor.capture());

    SanskritChange sanskritChange = sanskritChangeCaptor.getValue();
    MutableSanskritObject sanskritChangeValues = sanskrit.newMutableSanskritObject();
    sanskritChange.accept(sanskritChangeValues);

    assertThat(sanskritChangeValues.getLong("mutativeMessageCount"), is(expectedMutativeMessageCount));
    assertThat(sanskritChangeValues.getString("mode"), is("PREPARED"));
    assertThat(sanskritChangeValues.getString("latestChangeUuid"), is(uuid.toString()));
    assertThat(sanskritChangeValues.getLong("currentVersion"), is(2L));
    assertThat(sanskritChangeValues.getLong("highestVersion"), is(3L));
    assertThat(sanskritChangeValues.getString("lastMutationHost"), is("host2"));
    assertThat(sanskritChangeValues.getString("lastMutationUser"), is("user2"));

    SanskritObject changeDetails = sanskritChangeValues.getObject(uuid.toString());
    assertThat(changeDetails.getString("state"), is("COMMITTED"));
    assertThat(changeDetails.getLong("version"), is(4L));
    assertThat(changeDetails.getExternal("operation", NomadChange.class), is(settingNomadChange));
    assertThat(changeDetails.getString("changeResultHash"), is("c2c9b194778150614a8a1b127842fc6f42b1a5f4"));
    assertThat(changeDetails.getString("creationHost"), is("host1"));
    assertThat(changeDetails.getString("creationUser"), is("user1"));

    verify(configStorage).saveConfig(4L, "config");
  }

  @Test
  public void updateChangeRequestState() throws Exception {
    UUID uuid = UUID.randomUUID();

    MutableSanskritObject currentChangeState = sanskrit.newMutableSanskritObject();
    currentChangeState.setString("state", "PREPARED");
    currentChangeState.setString("field", "value");
    when(sanskrit.getObject(uuid.toString())).thenReturn(currentChangeState);

    state.applyStateChange(state.newStateChange().updateChangeRequestState(uuid, ROLLED_BACK));

    verify(sanskrit).applyChange(sanskritChangeCaptor.capture());

    SanskritChange sanskritChange = sanskritChangeCaptor.getValue();
    MutableSanskritObject sanskritChangeValues = sanskrit.newMutableSanskritObject();
    sanskritChange.accept(sanskritChangeValues);
    SanskritObject changeDetails = sanskritChangeValues.getObject(uuid.toString());

    assertThat(changeDetails.getString("state"), is("ROLLED_BACK"));
    assertThat(changeDetails.getString("field"), is("value"));
  }

  @Test
  public void getCurrentCommittedChangeResult() throws Exception {
    when(sanskrit.getLong("currentVersion")).thenReturn(5L);
    when(configStorage.getConfig(5L)).thenReturn("config");
    assertEquals("config", state.getCurrentCommittedChangeResult());
  }
}
