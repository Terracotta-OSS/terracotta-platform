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
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author Mathieu Carbou
 */
@Ignore // TODO activate
public class HAIT extends AbstractHATest {

  @Test
  public void topology_includes_passives() throws Exception {
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

    Server passive = cluster.serverStream().filter(server -> !server.isActive()).findFirst().get();
    final String[] currentPassive = {toJson(passive.toMap()).toString()};
    cluster.clientStream().forEach(client -> currentPassive[0] = currentPassive[0]
        .replace(passive.getServerName(), "stripe-PASSIVE"));

    // and compare
    assertEquals(readJson("passive.json").toString(), currentPassive[0]);
  }

  @Test
  public void get_notifications_when_passive_leaves() throws Exception {
    Server active = tmsAgentService.readTopology().serverStream().filter(Server::isActive).findFirst().get();
    Server passive = tmsAgentService.readTopology().serverStream().filter(server -> !server.isActive()).findFirst().get();
    assertThat(active.getState(), equalTo(Server.State.ACTIVE));
    assertThat(passive.getState(), equalTo(Server.State.PASSIVE));

    // clear notification buffer
    tmsAgentService.readMessages();

    // remove one passive
    voltron.getClusterControl().terminateOnePassive();

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

}
