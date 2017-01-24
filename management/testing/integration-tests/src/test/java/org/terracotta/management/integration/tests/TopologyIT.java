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
import org.terracotta.management.model.capabilities.descriptors.Settings;
import org.terracotta.management.model.cluster.Client;
import org.terracotta.management.model.cluster.Cluster;
import org.terracotta.management.model.cluster.ServerEntity;
import org.terracotta.management.model.message.Message;
import org.terracotta.management.model.notification.ContextualNotification;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author Mathieu Carbou
 */
public class TopologyIT extends AbstractSingleTest {

  @Test
  public void can_read_topology() throws Exception {
    Cluster cluster = tmsAgentService.readTopology();
    String currentTopo = toJson(cluster.toMap()).toString();
    String actual = removeRandomValues(currentTopo);
    String expected = readJson("topology.json").toString();
    System.out.println(actual);
    assertEquals(expected, actual);
  }

  @Test
  public void can_read_messages() throws Exception {
    List<Message> messages = tmsAgentService.readMessages();

    Map<String, List<Message>> messsageByTypes = messages.stream().collect(Collectors.groupingBy(Message::getType));
    assertThat(messsageByTypes.size(), equalTo(2));
    assertThat(messsageByTypes.get("TOPOLOGY").size(), equalTo(1));

    List<ContextualNotification> notifs = messages.stream()
        .filter(message -> message.getType().equals("NOTIFICATION"))
        .flatMap(message -> message.unwrap(ContextualNotification.class).stream())
        .collect(Collectors.toList());

    Map<String, List<ContextualNotification>> notifsByTypes = notifs.stream().collect(Collectors.groupingBy(ContextualNotification::getType));

    assertThat(notifsByTypes.get("CLIENT_CACHE_CREATED").size(), equalTo(4));
    assertThat(notifsByTypes.get("CLIENT_INIT").size(), equalTo(2));
    assertThat(notifsByTypes.get("SERVER_ENTITY_CREATED").size(), equalTo(4));
    assertThat(notifsByTypes.get("SERVER_CACHE_CREATED").size(), equalTo(2));
    assertThat(notifsByTypes.get("CLIENT_REGISTRY_AVAILABLE").size(), equalTo(2));
    assertThat(notifsByTypes.get("CLIENT_CONNECTED").size(), equalTo(2));
    assertThat(notifsByTypes.get("CLIENT_TAGS_UPDATED").size(), equalTo(2));
    assertThat(notifsByTypes.get("SERVER_ENTITY_FETCHED").size(), equalTo(7));
    assertThat(notifsByTypes.get("ENTITY_REGISTRY_AVAILABLE").size(), equalTo(3));
    assertThat(notifsByTypes.get("CLIENT_ATTACHED").size(), equalTo(4));
    assertThat(notifsByTypes.get("CLIENT_DETACHED"), is(nullValue()));
  }

  @Test
  public void notifications_have_a_source_context() throws Exception {
    List<Message> messages = tmsAgentService.readMessages();
    List<ContextualNotification> notifs = messages.stream()
        .filter(message -> message.getType().equals("NOTIFICATION"))
        .flatMap(message -> message.unwrap(ContextualNotification.class).stream())
        .collect(Collectors.toList());

    String currentJson = toJson(notifs).toString();
    String actual = removeRandomValues(currentJson);
    String expected = readJson("notifications.json").toString();

    assertEquals(expected, actual);
  }

}
