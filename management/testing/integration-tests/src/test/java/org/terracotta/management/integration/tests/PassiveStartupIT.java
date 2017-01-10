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
import org.terracotta.management.model.message.Message;
import org.terracotta.management.model.notification.ContextualNotification;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
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
    tmsAgentService.readMessages();
  }

  @Test
  public void get_notifications_when_passive_joins() throws Exception {
    put(0, "clients", "client1", "Mat");

    // start a passive after management is connected to active
    voltron.getClusterControl().startOneServer();
    voltron.getClusterControl().waitForRunningPassivesInStandby();

    put(0, "clients", "client2", "Garfield");

    assertThat(get(1, "clients", "client1"), equalTo("Mat"));

    Server active = tmsAgentService.readTopology().serverStream().filter(Server::isActive).findFirst().get();
    Server passive = tmsAgentService.readTopology().serverStream().filter(server -> !server.isActive()).findFirst().get();
    assertThat(active.getState(), equalTo(Server.State.ACTIVE));
    assertThat(passive.getState(), equalTo(Server.State.PASSIVE));

    // read messages
    Map<String, List<Message>> map = tmsAgentService.readMessages().stream().collect(Collectors.groupingBy(Message::getType));
    assertThat(map.size(), equalTo(2));
    assertThat(map.keySet(), hasItem("TOPOLOGY"));
    assertThat(map.keySet(), hasItem("NOTIFICATION"));

    List<ContextualNotification> notifs = new ArrayList<>();
    List<String> states = new ArrayList<>();

    map.get("NOTIFICATION").stream()
        .flatMap(message -> message.unwrap(ContextualNotification.class).stream())
        .forEach(notifs::add);

    notifs.stream()
        .filter(contextualNotification -> contextualNotification.getType().equals("SERVER_STATE_CHANGED"))
        .map(contextualNotification -> contextualNotification.getAttributes().get("state"))
        .forEach(states::add);

    while (!Thread.currentThread().isInterrupted()
        && !(states.contains("SYNCHRONIZING") && states.contains("PASSIVE"))
        && notifs.stream().noneMatch(contextualNotification -> contextualNotification.getType().equals("SYNC_END"))) {
      tmsAgentService.readMessages().stream()
          .filter(message -> message.getType().equals("NOTIFICATION"))
          .flatMap(message -> message.unwrap(ContextualNotification.class).stream())
          .forEach(contextualNotification -> {
            notifs.add(contextualNotification);
            if (contextualNotification.getType().equals("SERVER_STATE_CHANGED")) {
              states.add(contextualNotification.getAttributes().get("state"));
            }
          });
    }

    // test notifications generated on active from passive "actions"
    notifs.forEach(System.out::println);
    assertThat(
        notifs.stream().map(ContextualNotification::getType).collect(Collectors.toList()),
        hasItems(
            "SERVER_JOINED",
            "SERVER_STATE_CHANGED",
            "SERVER_ENTITY_CREATED",
            "SERVER_ENTITY_CREATED", "ENTITY_REGISTRY_AVAILABLE",
            "SERVER_ENTITY_CREATED", "ENTITY_REGISTRY_AVAILABLE",
            "SERVER_ENTITY_CREATED", "ENTITY_REGISTRY_AVAILABLE",
            "SERVER_STATE_CHANGED",
            "SYNC_END"
        ));

    // only 1 server in source: passive server
    assertThat(
        notifs.stream().map(contextualNotification -> contextualNotification.getContext().get(Server.NAME_KEY)).collect(Collectors.toSet()),
        equalTo(new HashSet<>(Arrays.asList(passive.getServerName()))));

    // test state transition of passive
    assertThat(
        states,
        hasItems("SYNCHRONIZING", "PASSIVE"));


    assertThat(notifs.stream()
            .map(contextualNotification -> contextualNotification.getContext().get(Server.NAME_KEY))
            .collect(Collectors.toSet()),
        equalTo(new HashSet<>(Arrays.asList(passive.getServerName(), passive.getServerName()))));
  }

}
