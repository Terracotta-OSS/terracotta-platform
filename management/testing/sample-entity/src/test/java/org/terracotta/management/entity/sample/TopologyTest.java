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
package org.terracotta.management.entity.sample;

import org.junit.Test;
import org.terracotta.management.model.capabilities.descriptors.Settings;
import org.terracotta.management.model.cluster.Cluster;
import org.terracotta.management.model.cluster.ServerEntity;
import org.terracotta.management.model.message.Message;
import org.terracotta.management.model.notification.ContextualNotification;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author Mathieu Carbou
 */
public class TopologyTest extends AbstractTest {

  @Test
  public void can_read_topology() throws Exception {
    Cluster cluster = tmsAgent.readTopology().get();

    // removes all random values

    cluster.serverStream().forEach(server -> {
      server.setActivateTime(0);
      server.setStartTime(0);
      server.setBuildId("Build ID");
    });

    cluster.serverEntityStream()
        .map(ServerEntity::getManagementRegistry)
        .map(managementRegistry -> managementRegistry.flatMap(r -> r.getCapability("ServerCacheSettings")))
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
        .replace(client.getHostName(), "<hostname>"));

    // and compare

    assertEquals(readJson("topology.json").toString(), currentTopo[0]);
  }

  @Test
  public void can_read_messages() throws Exception {
    List<Message> messages = tmsAgent.readMessages().get();

    Map<String, List<Message>> messsageByTypes = messages.stream().collect(Collectors.groupingBy(Message::getType));
    assertThat(messsageByTypes.size(), equalTo(2));
    assertThat(messsageByTypes.get("NOTIFICATION").size(), equalTo(35));
    assertThat(messsageByTypes.get("TOPOLOGY").size(), equalTo(1));

    List<ContextualNotification> notifs = messages.stream()
        .filter(message -> message.getType().equals("NOTIFICATION"))
        .flatMap(message -> message.unwrap(ContextualNotification.class).stream())
        .collect(Collectors.toList());

    Map<String, List<ContextualNotification>> notifsByTypes = notifs.stream().collect(Collectors.groupingBy(ContextualNotification::getType));
    assertThat(notifsByTypes.size(), equalTo(11));

    assertThat(notifsByTypes.get("CLIENT_REGISTRY_UPDATED").size(), equalTo(4));
    assertThat(notifsByTypes.get("CLIENT_CACHE_CREATED").size(), equalTo(4));
    assertThat(notifsByTypes.get("CLIENT_INIT").size(), equalTo(2));
    assertThat(notifsByTypes.get("SERVER_ENTITY_CREATED").size(), equalTo(4));
    assertThat(notifsByTypes.get("SERVER_CACHE_CREATED").size(), equalTo(2));
    assertThat(notifsByTypes.get("CLIENT_REGISTRY_AVAILABLE").size(), equalTo(2));
    assertThat(notifsByTypes.get("CLIENT_CONNECTED").size(), equalTo(2));
    assertThat(notifsByTypes.get("ENTITY_REGISTRY_UPDATED").size(), equalTo(2));
    assertThat(notifsByTypes.get("CLIENT_TAGS_UPDATED").size(), equalTo(2));
    assertThat(notifsByTypes.get("SERVER_ENTITY_FETCHED").size(), equalTo(8));
    assertThat(notifsByTypes.get("ENTITY_REGISTRY_AVAILABLE").size(), equalTo(3));
  }

  @Test
  public void notifications_have_a_source_context() throws Exception {
    List<Message> messages = tmsAgent.readMessages().get();
    List<ContextualNotification> notifs = messages.stream()
        .filter(message -> message.getType().equals("NOTIFICATION"))
        .flatMap(message -> message.unwrap(ContextualNotification.class).stream())
        .collect(Collectors.toList());

    // removes all random values

    final String[] currentJson = {toJson(notifs).toString()};

    tmsAgent.readTopology().get().clientStream().forEach(client -> currentJson[0] = currentJson[0]
        .replace(client.getClientIdentifier().getConnectionUid(), "<uuid>")
        .replace(String.valueOf(client.getPid()), "0")
        .replace(String.valueOf(client.connectionStream().findFirst().get().getClientEndpoint().getPort()), "0")
        .replace(client.getHostName(), "<hostname>"));


    assertEquals(readJson("notifications.json").toString(), currentJson[0]);
  }

}
