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
import org.terracotta.management.model.capabilities.descriptors.Settings;
import org.terracotta.management.model.cluster.Cluster;
import org.terracotta.management.model.cluster.Server;
import org.terracotta.management.model.cluster.ServerEntity;
import org.terracotta.management.model.message.Message;
import org.terracotta.management.model.notification.ContextualNotification;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.equalTo;
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

    // kill active - passive should take the active role
    voltron.getClusterControl().terminateActive();
    voltron.getClusterControl().waitForActive();
  }

  @Test
  @Ignore
  public void topology_recovery_after_failover() throws Exception {
    Cluster cluster = tmsAgentService.readTopology();

    // verify new server
    Server newActive = cluster.serverStream().filter(Server::isActive).findFirst().get();
    assertThat(newActive.getState(), equalTo(Server.State.ACTIVE));
    assertThat(newActive.getServerName(), equalTo(oldPassive.getServerName()));

    // removes all random values
    String currentTopo = toJson(cluster.toMap()).toString();
    String actual = removeRandomValues(currentTopo);

    String expected = readJson("topology.json").toString();
    assertEquals(expected, actual);
  }

  @Test
  @Ignore
  public void notifications_after_failover() throws Exception {
    // read messages
    List<Message> messages = tmsAgentService.readMessages();

    messages.forEach(System.out::println);

    assertThat(messages.size(), equalTo(3));

    List<ContextualNotification> notifs = messages.stream()
        .filter(message -> message.getType().equals("NOTIFICATION"))
        .flatMap(message -> message.unwrap(ContextualNotification.class).stream())
        .filter(notif -> notif.getType().equals("SERVER_STATE_CHANGED"))
        .collect(Collectors.toList());

    assertThat(
        notifs.stream().map(notif -> notif.getContext().get(Server.NAME_KEY)).collect(Collectors.toList()),
        equalTo(Arrays.asList(oldPassive.getServerName(), oldPassive.getServerName())));

    assertThat(
        notifs.stream().map(notif -> notif.getAttributes().get("state")).collect(Collectors.toList()),
        equalTo(Arrays.asList("ACTIVE", "ACTIVE")));

    //TODO: complete with Galvan
    //- test topology (like topology_includes_passives), client should have re-exposed their management metadata
    //- check notifications: server states
    //- check notification that might be there: CLIENT_RECONNECTED and SERVER_ENTITY_FAILOVER_COMPLETE
  }

  @Test
  @Ignore
  public void puts_can_be_seen_on_other_clients_after_failover() throws Exception {
    put(0, "clients", "client1", "Mathieu");

    // kill active - passive should take the active role
    voltron.getClusterControl().terminateActive();
    voltron.getClusterControl().waitForActive();

    assertThat(get(1, "clients", "client1"), equalTo("Mathieu"));
  }

}
