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

import org.junit.Test;
import org.terracotta.management.model.cluster.Server;
import org.terracotta.management.model.message.Message;
import org.terracotta.management.model.notification.ContextualNotification;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Mathieu Carbou
 */
public class PassiveLeaveIT extends AbstractHATest {

  @Test
  public void get_notifications_when_passive_leaves() throws Exception {
    Server active = nmsService.readTopology().serverStream().filter(Server::isActive).findFirst().get();
    Server passive = nmsService.readTopology().serverStream().filter(server -> !server.isActive()).findFirst().get();
    assertThat(active.getState(), equalTo(Server.State.ACTIVE));
    assertThat(passive.getState(), equalTo(Server.State.PASSIVE));

    // clear notification buffer
    nmsService.readMessages();

    // remove one passive
    voltron.getClusterControl().terminateOnePassive();

    // wait for SERVER_LEFT message
    List<Message> messages;
    do {
      messages = nmsService.readMessages();

      if (messages.stream()
          .filter(message -> message.getType().equals("NOTIFICATION"))
          .flatMap(message -> message.unwrap(ContextualNotification.class).stream())
          .map(ContextualNotification::getType)
          .anyMatch(s -> s.equals("SERVER_LEFT"))) {
        break;
      }
    }
    while (!Thread.currentThread().isInterrupted());

    assertThat(messages.stream()
            .filter(message -> message.getType().equals("NOTIFICATION"))
            .flatMap(message -> message.unwrap(ContextualNotification.class).stream())
            .map(ContextualNotification::getType)
            .anyMatch(s -> s.equals("SERVER_LEFT")),
        is(true));

    assertThat(messages.stream()
            .filter(message -> message.getType().equals("NOTIFICATION"))
            .flatMap(message -> message.unwrap(ContextualNotification.class).stream())
            .filter(notif -> notif.getType().equals("SERVER_LEFT"))
            .map(contextualNotification -> contextualNotification.getContext().get(Server.NAME_KEY))
            .collect(Collectors.toList()),
        equalTo(Arrays.asList(passive.getServerName())));
  }

}
