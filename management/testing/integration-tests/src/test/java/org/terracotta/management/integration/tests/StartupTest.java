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
package org.terracotta.management.integration.tests;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.terracotta.management.model.cluster.Cluster;
import org.terracotta.management.model.cluster.Server;
import org.terracotta.management.model.message.Message;
import org.terracotta.management.model.notification.ContextualNotification;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertThat;

/**
 * @author Mathieu Carbou
 */
@Ignore // TODO activate
public class StartupTest extends AbstractHATest {

  @Before
  @Override
  public void setUp() throws Exception {
    // this sequence is so tha twe can have a stripe of 2 servers bu starting with only 1 active
    // galvan does not have an easier way to do that
    voltron.getClusterControl().waitForActive();
    voltron.getClusterControl().waitForRunningPassivesInStandby();
    voltron.getClusterControl().terminateOnePassive();

    commonSetUp(voltron);
    tmsAgentService.readMessages();

    // start a passive after management is connected to active
    voltron.getClusterControl().startOneServer();
    voltron.getClusterControl().waitForRunningPassivesInStandby();
  }

  @Test
  public void get_notifications_when_passive_joins() throws Exception {
    Server active = tmsAgentService.readTopology().serverStream().filter(Server::isActive).findFirst().get();
    Server passive = tmsAgentService.readTopology().serverStream().filter(server -> !server.isActive()).findFirst().get();
    assertThat(active.getState(), equalTo(Server.State.ACTIVE));
    assertThat(passive.getState(), equalTo(Server.State.PASSIVE));

    // read messages
    List<Message> messages = tmsAgentService.readMessages();
    assertThat(messages.size(), equalTo(20));
    Map<String, List<Message>> map = messages.stream().collect(Collectors.groupingBy(Message::getType));
    assertThat(map.size(), equalTo(2));
    assertThat(map.keySet(), hasItem("TOPOLOGY"));
    assertThat(map.keySet(), hasItem("NOTIFICATION"));
    assertThat(map.get("NOTIFICATION").size(), equalTo(19));

    List<ContextualNotification> notifs = map.get("NOTIFICATION").stream()
        .flatMap(message -> message.unwrap(ContextualNotification.class).stream())
        .collect(Collectors.toList());

    assertThat(
        notifs.stream().map(ContextualNotification::getType).collect(Collectors.toList()),
        equalTo(Arrays.asList(
            "SERVER_JOINED",
            "SERVER_STATE_CHANGED", "SERVER_STATE_CHANGED",
            "SERVER_ENTITY_CREATED",
            "ENTITY_REGISTRY_AVAILABLE", "ENTITY_REGISTRY_UPDATED", "SERVER_CACHE_CREATED",
            "SYNC_START", "SYNC_END",
            "SERVER_ENTITY_CREATED", "ENTITY_REGISTRY_AVAILABLE", "SERVER_ENTITY_CREATED", "ENTITY_REGISTRY_AVAILABLE", "ENTITY_REGISTRY_UPDATED", "SERVER_CACHE_CREATED",
            "SYNC_START", "SYNC_END",
            "SERVER_ENTITY_CREATED", "SERVER_STATE_CHANGED")));

    List<ContextualNotification> serverStateNotifs = messages.stream()
        .filter(message -> message.getType().equals("NOTIFICATION"))
        .flatMap(message -> message.unwrap(ContextualNotification.class).stream())
        .filter(notif -> notif.getType().equals("SERVER_STATE_CHANGED"))
        .collect(Collectors.toList());

    assertThat(
        serverStateNotifs.stream().map(notif -> notif.getContext().get(Server.NAME_KEY)).collect(Collectors.toList()),
        equalTo(Arrays.asList(passive.getServerName(), passive.getServerName(), passive.getServerName())));

    assertThat(
        serverStateNotifs.stream().map(notif -> notif.getAttributes().get("state")).collect(Collectors.toList()),
        equalTo(Arrays.asList("UNINITIALIZED", "SYNCHRONIZING", "PASSIVE")));
  }

  @Test
  public void get_server_states_when_passive_joins() throws Exception {
    Server active = tmsAgentService.readTopology().serverStream().filter(Server::isActive).findFirst().get();
    Server passive = tmsAgentService.readTopology().serverStream().filter(server -> !server.isActive()).findFirst().get();
    assertThat(active.getState(), equalTo(Server.State.ACTIVE));
    assertThat(passive.getState(), equalTo(Server.State.PASSIVE));

    // read messages
    List<Message> messages = tmsAgentService.readMessages();
    List<ContextualNotification> notifs = messages.stream()
        .filter(message -> message.getType().equals("NOTIFICATION"))
        .flatMap(message -> message.unwrap(ContextualNotification.class).stream())
        .filter(notif -> notif.getType().equals("SERVER_STATE_CHANGED"))
        .collect(Collectors.toList());


  }

  @Test
  public void failover_management() throws Exception {
    Cluster cluster = tmsAgentService.readTopology();
    Server active = cluster.serverStream().filter(Server::isActive).findFirst().get();
    Server passive = cluster.serverStream().filter(server -> !server.isActive()).findFirst().get();
    assertThat(active.getState(), equalTo(Server.State.ACTIVE));
    assertThat(passive.getState(), equalTo(Server.State.PASSIVE));

    // clear buffer
    tmsAgentService.readMessages();

    // kill active - passive should take the active role
    voltron.getClusterControl().terminateActive();
    voltron.getClusterControl().waitForActive();

    cluster = tmsAgentService.readTopology();
    Server newActive = cluster.serverStream().filter(Server::isActive).findFirst().get();
    assertThat(newActive.getState(), equalTo(Server.State.ACTIVE));
    assertThat(newActive.getServerName(), equalTo(passive.getServerName()));

    // read messages
    List<Message> messages = tmsAgentService.readMessages();
    assertThat(messages.size(), equalTo(3));

    List<ContextualNotification> notifs = messages.stream()
        .filter(message -> message.getType().equals("NOTIFICATION"))
        .flatMap(message -> message.unwrap(ContextualNotification.class).stream())
        .filter(notif -> notif.getType().equals("SERVER_STATE_CHANGED"))
        .collect(Collectors.toList());

    assertThat(
        notifs.stream().map(notif -> notif.getContext().get(Server.NAME_KEY)).collect(Collectors.toList()),
        equalTo(Arrays.asList(newActive.getServerName(), newActive.getServerName())));

    assertThat(
        notifs.stream().map(notif -> notif.getAttributes().get("state")).collect(Collectors.toList()),
        equalTo(Arrays.asList("ACTIVE", "ACTIVE")));

    //TODO: complete with Galvan
    //- test topology (like topology_includes_passives), client should have re-exposed their management metadata
    //- check notifications: server states
    //- check notification that might be there: CLIENT_RECONNECTED and SERVER_ENTITY_FAILOVER_COMPLETE
  }

}
