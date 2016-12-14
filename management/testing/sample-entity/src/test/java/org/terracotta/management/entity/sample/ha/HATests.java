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
package org.terracotta.management.entity.sample.ha;

import org.junit.Ignore;
import org.junit.Test;
import org.terracotta.management.model.capabilities.descriptors.Settings;
import org.terracotta.management.model.cluster.Cluster;
import org.terracotta.management.model.cluster.Server;
import org.terracotta.management.model.cluster.ServerEntity;
import org.terracotta.management.model.message.Message;
import org.terracotta.management.model.notification.ContextualNotification;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Mathieu Carbou
 */
public class HATests extends AbstractHaTest {

  @Test
  public void topology_includes_passives() throws Exception {
    // connect passive
    stripeControl.startOneServer();
    stripeControl.waitForRunningPassivesInStandby();

    Cluster cluster = tmsAgentService.readTopology();

    // removes all random values

    cluster.serverStream().forEach(server -> {
      server.setActivateTime(0);
      server.setStartTime(0);
      server.setBuildId("Build ID");
    });

    cluster.serverEntityStream()
        .map(ServerEntity::getManagementRegistry)
        .flatMap(managementRegistry -> Stream.of(
            managementRegistry.flatMap(r -> r.getCapability("ServerCacheSettings")),
            managementRegistry.flatMap(r -> r.getCapability("OffHeapResourceSettings"))))
        .forEach(capability -> {
          if (capability.isPresent()) {
            capability.get()
                .getDescriptors(Settings.class)
                .stream()
                .filter(settings -> settings.containsKey("time")).forEach(settings -> settings.set("time", 0));
          }
        });

    final String[] currentTopo = {toJson(cluster.toMap()).toString()};
    cluster.clientStream().forEach(client -> currentTopo[0] = currentTopo[0]
        .replace(client.getClientIdentifier().getConnectionUid(), "<uuid>")
        .replace(String.valueOf(client.getPid()), "0")
        .replace(String.valueOf(client.connectionStream().findFirst().get().getClientEndpoint().getPort()), "0")
        .replace(client.getHostName(), "<hostname>")
        .replace(client.getHostAddress(), "127.0.0.1"));

    // and compare

    assertEquals(readJson("topology-ha.json").toString(), currentTopo[0]);
  }

  @Test
  //TODO: needs to confirm that with a galvan test: the sequence of notifications seems weird on passthrough. All notifs are there, but there are duplicates.
  public void get_notifications_when_passive_joins() throws Exception {
    // clear
    tmsAgentService.readMessages();

    // connect passive
    stripeControl.startOneServer();
    stripeControl.waitForRunningPassivesInStandby();

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
  }

  @Test
  public void get_notifications_when_passive_leaves() throws Exception {
    // connect passive
    stripeControl.startOneServer();
    stripeControl.waitForRunningPassivesInStandby();

    Server active = tmsAgentService.readTopology().serverStream().filter(Server::isActive).findFirst().get();
    Server passive = tmsAgentService.readTopology().serverStream().filter(server -> !server.isActive()).findFirst().get();
    assertThat(active.getState(), equalTo(Server.State.ACTIVE));
    assertThat(passive.getState(), equalTo(Server.State.PASSIVE));

    // clear notification buffer
    tmsAgentService.readMessages();

    // remove one passive
    stripeControl.terminateOnePassive();

    // read messages
    List<Message> messages = tmsAgentService.readMessages();
    assertThat(messages.size(), equalTo(2));
    Map<String, List<Message>> map = messages.stream().collect(Collectors.groupingBy(Message::getType));
    assertThat(map.size(), equalTo(2));
    assertThat(map.keySet(), hasItem("TOPOLOGY"));
    assertThat(map.keySet(), hasItem("NOTIFICATION"));
    assertThat(map.get("NOTIFICATION").size(), equalTo(1));

    List<ContextualNotification> notifs = map.get("NOTIFICATION").stream()
        .flatMap(message -> message.unwrap(ContextualNotification.class).stream())
        .collect(Collectors.toList());

    assertThat(
        notifs.stream().map(ContextualNotification::getType).collect(Collectors.toList()),
        equalTo(Arrays.asList("SERVER_LEFT")));

    assertThat(
        notifs.get(0).getContext().get(Server.NAME_KEY),
        equalTo(passive.getServerName()));
  }

  @Test
  public void get_server_states_when_passive_joins() throws Exception {
    // clear
    tmsAgentService.readMessages();

    // connect passive
    stripeControl.startOneServer();
    stripeControl.waitForRunningPassivesInStandby();

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

    assertThat(
        notifs.stream().map(notif -> notif.getContext().get(Server.NAME_KEY)).collect(Collectors.toList()),
        equalTo(Arrays.asList(passive.getServerName(), passive.getServerName(), passive.getServerName())));

    assertThat(
        notifs.stream().map(notif -> notif.getAttributes().get("state")).collect(Collectors.toList()),
        equalTo(Arrays.asList("UNINITIALIZED", "SYNCHRONIZING", "PASSIVE")));
  }

  @Test
  @Ignore("Will be done for https://github.com/Terracotta-OSS/terracotta-platform/issues/171")
  //TODO: support failover: https://github.com/Terracotta-OSS/terracotta-platform/issues/171
  public void can_failover() throws Exception {
    fail();
  }

}
