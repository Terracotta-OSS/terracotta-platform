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
import org.junit.Test;
import org.terracotta.management.model.cluster.Server;
import org.terracotta.management.model.notification.ContextualNotification;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Mathieu Carbou
 */
public class PassiveStartupIT extends AbstractHATest {

  @Before
  @Override
  public void setUp() throws Exception {
    System.out.println(" => [" + testName.getMethodName() + "] " + getClass().getSimpleName() + ".setUp()");

    // this sequence is so tha twe can have a stripe of 2 servers bu starting with only 1 active
    // galvan does not have an easier way to do that
    voltron.getClusterControl().waitForActive();
    voltron.getClusterControl().waitForRunningPassivesInStandby();
    voltron.getClusterControl().terminateOnePassive();

    commonSetUp(voltron);
    nmsService.readMessages();
  }

  @Test
  public void get_notifications_when_passive_joins() throws Exception {
    put(0, "clients", "client1", "Mat");

    // start a passive after management is connected to active
    voltron.getClusterControl().startOneServer();
    voltron.getClusterControl().waitForRunningPassivesInStandby();

    put(0, "clients", "client2", "Garfield");

    assertThat(get(1, "clients", "client1"), equalTo("Mat"));

    Server active = nmsService.readTopology().serverStream().filter(Server::isActive).findFirst().get();
    Server passive = nmsService.readTopology().serverStream().filter(server -> !server.isActive()).findFirst().get();
    assertThat(active.getState(), equalTo(Server.State.ACTIVE));
    assertThat(passive.getState(), equalTo(Server.State.PASSIVE));

    // read messages
    List<ContextualNotification> collected = waitForAllNotifications("SERVER_JOINED",
        "SERVER_STATE_CHANGED",
        "SERVER_ENTITY_CREATED", "ENTITY_REGISTRY_AVAILABLE",
        "SERVER_STATE_CHANGED",
        "SYNC_END");

    Predicate<ContextualNotification> gotPassive = n -> n.getType().equals("SERVER_STATE_CHANGED") && "PASSIVE".equals(n.getAttributes().get("state"));
    boolean gotPassiveNotif = collected.stream().anyMatch(gotPassive);
    if (!gotPassiveNotif) {
      nmsService.waitForMessage(message -> message.getType().equals("NOTIFICATION") && message.unwrap(ContextualNotification.class).stream().anyMatch(gotPassive));
    }

    Set<String> states = collected.stream()
        .filter(contextualNotification -> contextualNotification.getType().equals("SERVER_STATE_CHANGED"))
        .map(contextualNotification -> contextualNotification.getAttributes().get("state"))
        .collect(Collectors.toSet());

    assertThat(states.contains("SYNCHRONIZING"), is(true));
    assertThat(states.contains("PASSIVE"), is(true));

    // only 1 server in source: passive server
    ContextualNotification stateChanged = collected.stream().filter(n -> n.getType().equals("SERVER_STATE_CHANGED")).findFirst().get();
    assertThat(stateChanged.getContext().get(Server.NAME_KEY), equalTo(passive.getServerName()));
  }

}
