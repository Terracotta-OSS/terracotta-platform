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
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author Mathieu Carbou
 */
public class FailoverIT extends AbstractHATest {

  Server oldActive;
  Server oldPassive;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();

    Cluster cluster = tmsAgentService.readTopology();
    oldActive = cluster.serverStream().filter(Server::isActive).findFirst().get();
    oldPassive = cluster.serverStream().filter(server -> !server.isActive()).findFirst().get();
    assertThat(oldActive.getState(), equalTo(Server.State.ACTIVE));
    assertThat(oldPassive.getState(), equalTo(Server.State.PASSIVE));

    // clear buffer
    tmsAgentService.readMessages();

    // add some data from client 0
    put(0, "clients", "client1", "Mathieu");

    // kill active - passive should take the active role
    voltron.getClusterControl().terminateActive();
    voltron.getClusterControl().waitForActive();
  }

  @Test
  @Ignore("See https://github.com/Terracotta-OSS/terracotta-core/issues/412")
  public void all_registries_reexposed_after_failover() throws Exception {
    Cluster cluster = tmsAgentService.readTopology();

    // removes all random values
    String currentTopo = toJson(cluster.toMap()).toString();
    String actual = removeRandomValues(currentTopo);

    System.out.println(actual);

    assertEquals(readJson("topology-after-failover.json"), readJsonStr(actual));
  }

  @Test
  public void notifications_after_failover() throws Exception {
    // read messages
    List<Message> messages = tmsAgentService.readMessages();

    List<ContextualNotification> notifs = messages.stream()
        .filter(message -> message.getType().equals("NOTIFICATION"))
        .flatMap(message -> message.unwrap(ContextualNotification.class).stream())
        .collect(Collectors.toList());

    assertThat(
        notifs.stream().map(ContextualNotification::getType).collect(Collectors.toList()),
        hasItems(
            "SERVER_JOINED", "SERVER_STATE_CHANGED",
            "SERVER_ENTITY_CREATED", "SERVER_ENTITY_CREATED", "SERVER_ENTITY_CREATED", "SERVER_ENTITY_CREATED",
            "ENTITY_REGISTRY_AVAILABLE", "ENTITY_REGISTRY_AVAILABLE", "ENTITY_REGISTRY_AVAILABLE",
            "CLIENT_CONNECTED", "CLIENT_CONNECTED", "CLIENT_CONNECTED",
            "SERVER_ENTITY_FETCHED", "SERVER_ENTITY_FETCHED", "SERVER_ENTITY_FETCHED", "SERVER_ENTITY_FETCHED"));

    assertThat(notifs.get(1).getContext().get(Server.NAME_KEY), equalTo(oldPassive.getServerName()));
    assertThat(notifs.get(1).getAttributes().get("state"), equalTo("ACTIVE"));
  }

  @Test
  public void puts_can_be_seen_on_other_clients_after_failover() throws Exception {
    assertThat(get(1, "clients", "client1"), equalTo("Mathieu"));
  }

}
