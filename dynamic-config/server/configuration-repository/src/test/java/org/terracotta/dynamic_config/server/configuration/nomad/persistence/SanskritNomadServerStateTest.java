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
package org.terracotta.dynamic_config.server.configuration.nomad.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.terracotta.dynamic_config.api.json.DynamicConfigApiJsonModule;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.Testing;
import org.terracotta.dynamic_config.api.model.Version;
import org.terracotta.dynamic_config.api.model.nomad.Applicability;
import org.terracotta.dynamic_config.api.model.nomad.SettingNomadChange;
import org.terracotta.dynamic_config.api.service.OssClusterValidator;
import org.terracotta.json.ObjectMapperFactory;
import org.terracotta.nomad.client.change.NomadChange;
import org.terracotta.nomad.server.ChangeRequest;
import org.terracotta.persistence.sanskrit.MutableSanskritObject;
import org.terracotta.persistence.sanskrit.ObjectMapperSupplier;
import org.terracotta.persistence.sanskrit.Sanskrit;
import org.terracotta.persistence.sanskrit.SanskritObject;
import org.terracotta.persistence.sanskrit.SanskritObjectImpl;
import org.terracotta.persistence.sanskrit.change.SanskritChange;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.terracotta.dynamic_config.api.model.Setting.OFFHEAP_RESOURCES;
import static org.terracotta.dynamic_config.api.model.Testing.newTestStripe;
import static org.terracotta.nomad.server.ChangeRequestState.COMMITTED;
import static org.terracotta.nomad.server.ChangeRequestState.ROLLED_BACK;
import static org.terracotta.nomad.server.NomadServerMode.ACCEPTING;
import static org.terracotta.nomad.server.NomadServerMode.PREPARED;

@RunWith(MockitoJUnitRunner.class)
public class SanskritNomadServerStateTest {

  NodeContext topology = new NodeContext(Testing.newTestCluster("bar", newTestStripe("stripe1").addNodes(Testing.newTestNode("node-1", "localhost"))), Testing.N_UIDS[1]);

  @Mock
  private Sanskrit sanskrit;

  @Mock
  private ConfigStorage configStorage;

  @Captor
  private ArgumentCaptor<SanskritChange> sanskritChangeCaptor;

  private SanskritNomadServerState state;
  private Instant now = Instant.now();

  @Before
  public void before() {
    Testing.replaceUIDs(topology.getCluster());
    new OssClusterValidator().validate(topology.getCluster());
    ObjectMapper objectMapper = new ObjectMapperFactory().withModule(new DynamicConfigApiJsonModule()).create();
    when(sanskrit.newMutableSanskritObject()).thenReturn(new SanskritObjectImpl(ObjectMapperSupplier.notVersioned(objectMapper)));
    state = new SanskritNomadServerState(sanskrit, configStorage, new DefaultHashComputer());
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

    SettingNomadChange settingNomadChange = SettingNomadChange.set(Applicability.cluster(), OFFHEAP_RESOURCES, "primary-server-resource", "2GB");

    MutableSanskritObject changeObject = sanskrit.newMutableSanskritObject();
    changeObject.setString("state", "ROLLED_BACK");
    changeObject.setLong("version", 1L);
    changeObject.setExternal("operation", settingNomadChange, Version.CURRENT.getValue());
    changeObject.setString("changeResultHash", "1063a7c79380cc1c8372c1f78d1104eefdeed073");
    changeObject.setString("creationHost", "host");
    changeObject.setString("creationUser", "user");
    changeObject.setString("creationTimestamp", now.toString());
    changeObject.setString("summary", "description");

    when(sanskrit.getObject(uuid.toString())).thenReturn(changeObject);
    when(configStorage.getConfig(1L)).thenReturn(new Config(topology, Version.CURRENT));

    ChangeRequest<NodeContext> changeRequest = state.getChangeRequest(uuid);
    SettingNomadChange change = (SettingNomadChange) changeRequest.getChange();

    assertEquals(ROLLED_BACK, changeRequest.getState());
    assertEquals(1L, changeRequest.getVersion());
    assertEquals(settingNomadChange, change);
    assertEquals(topology, changeRequest.getChangeResult());
    assertEquals("host", changeRequest.getCreationHost());
    assertEquals("user", changeRequest.getCreationUser());
    assertEquals(now, changeRequest.getCreationTimestamp());
    assertNull(changeRequest.getPrevChangeId());
    assertEquals("set offheap-resources.primary-server-resource=2GB", change.getSummary());
  }

  @Test
  public void getChangeRequestWithPrevChangeId() throws Exception {
    UUID uuid = UUID.randomUUID();
    UUID prevuuid = UUID.randomUUID();
    SettingNomadChange settingNomadChange = SettingNomadChange.set(Applicability.cluster(), OFFHEAP_RESOURCES, "primary-server-resource", "2GB");

    MutableSanskritObject changeObject = sanskrit.newMutableSanskritObject();
    changeObject.setString("state", "ROLLED_BACK");
    changeObject.setLong("version", 1L);
    changeObject.setString("prevChangeUuid", prevuuid.toString());
    changeObject.setExternal("operation", settingNomadChange, Version.CURRENT.getValue());
    changeObject.setString("changeResultHash", "1063a7c79380cc1c8372c1f78d1104eefdeed073");
    changeObject.setString("creationHost", "host");
    changeObject.setString("creationUser", "user");
    changeObject.setString("creationTimestamp", now.toString());
    changeObject.setString("summary", "description");

    when(sanskrit.getObject(uuid.toString())).thenReturn(changeObject);
    when(configStorage.getConfig(1L)).thenReturn(new Config(topology, Version.CURRENT));

    ChangeRequest<NodeContext> changeRequest = state.getChangeRequest(uuid);
    SettingNomadChange change = (SettingNomadChange) changeRequest.getChange();

    assertEquals(ROLLED_BACK, changeRequest.getState());
    assertEquals(1L, changeRequest.getVersion());
    assertEquals(settingNomadChange, change);
    assertEquals(topology, changeRequest.getChangeResult());
    assertEquals("host", changeRequest.getCreationHost());
    assertEquals("user", changeRequest.getCreationUser());
    assertEquals(now, changeRequest.getCreationTimestamp());
    assertEquals(prevuuid.toString(), changeRequest.getPrevChangeId());
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

    SettingNomadChange settingNomadChange = SettingNomadChange.set(Applicability.cluster(), OFFHEAP_RESOURCES, "primary-server-resource", "2GB");

    ChangeRequest<NodeContext> changeRequest = new ChangeRequest<>(
        COMMITTED,
        4L,
        null,
        settingNomadChange,
        topology,
        "host1",
        "user1",
        now
    );

    state.applyStateChange(state.newStateChange()
        .setMode(PREPARED)
        .setLatestChangeUuid(uuid)
        .setCurrentVersion(2L)
        .setHighestVersion(3L)
        .setLastMutationHost("host2")
        .setLastMutationUser("user2")
        .setLastMutationTimestamp(now)
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
    assertThat(sanskritChangeValues.getString("lastMutationTimestamp"), is(now.toString()));

    SanskritObject changeDetails = sanskritChangeValues.getObject(uuid.toString());
    assertThat(changeDetails.getString("state"), is("COMMITTED"));
    assertThat(changeDetails.getLong("version"), is(4L));
    assertThat(changeDetails.getObject("operation", NomadChange.class, null), is(settingNomadChange));
    assertThat(changeDetails.getString("changeResultHash"), is("1063a7c79380cc1c8372c1f78d1104eefdeed073"));
    assertThat(changeDetails.getString("creationHost"), is("host1"));
    assertThat(changeDetails.getString("creationUser"), is("user1"));
    assertThat(changeDetails.getString("creationTimestamp"), is(now.toString()));

    verify(configStorage).saveConfig(4L, topology);
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
    when(configStorage.getConfig(5L)).thenReturn(new Config(topology, Version.CURRENT));
    assertEquals(topology, state.getCurrentCommittedChangeResult().get());
  }
}
