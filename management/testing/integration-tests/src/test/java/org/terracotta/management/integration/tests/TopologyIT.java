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
import org.terracotta.management.model.cluster.Cluster;
import org.terracotta.management.model.message.Message;
import org.terracotta.management.model.notification.ContextualNotification;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

/**
 * @author Mathieu Carbou
 */
public class TopologyIT extends AbstractSingleTest {

  @Test
  public void can_read_topology() throws Exception {
    Cluster cluster = nmsService.readTopology();
    String currentTopo = toJson(cluster.toMap()).toString();
    String actual = removeRandomValues(currentTopo);
    String expected = readJson("topology.json").toString();
    assertEquals(expected, actual);
  }

  @Test
  public void can_read_messages() throws Exception {
    waitForAllNotifications(
        "CLIENT_CACHE_CREATED", "CLIENT_CACHE_CREATED", "CLIENT_CACHE_CREATED", "CLIENT_CACHE_CREATED",
        "CLIENT_INIT", "CLIENT_INIT",
        "SERVER_ENTITY_CREATED", "SERVER_ENTITY_CREATED", "SERVER_ENTITY_CREATED",
        "SERVER_CACHE_CREATED", "SERVER_CACHE_CREATED",
        "CLIENT_REGISTRY_AVAILABLE", "CLIENT_REGISTRY_AVAILABLE",
        "CLIENT_CONNECTED", "CLIENT_CONNECTED",
        "CLIENT_TAGS_UPDATED", "CLIENT_TAGS_UPDATED",
        "SERVER_ENTITY_FETCHED", "SERVER_ENTITY_FETCHED",
        "ENTITY_REGISTRY_AVAILABLE", "ENTITY_REGISTRY_AVAILABLE",
        "CLIENT_ATTACHED", "CLIENT_ATTACHED");
  }

  @Test
  public void notifications_have_a_source_context() throws Exception {
    List<ContextualNotification> notifs = waitForAllNotifications(
        "ENTITY_REGISTRY_AVAILABLE", "SERVER_ENTITY_FETCHED", 
        "CLIENT_CONNECTED", "SERVER_ENTITY_CREATED", "SERVER_ENTITY_FETCHED", "CLIENT_REGISTRY_AVAILABLE",
        "CLIENT_TAGS_UPDATED", "CLIENT_INIT", "CLIENT_CONNECTED", "SERVER_ENTITY_FETCHED", "CLIENT_REGISTRY_AVAILABLE",
        "CLIENT_TAGS_UPDATED", "CLIENT_INIT", "SERVER_ENTITY_CREATED", "ENTITY_REGISTRY_AVAILABLE", "SERVER_CACHE_CREATED",
        "SERVER_ENTITY_FETCHED", "CLIENT_ATTACHED", "CLIENT_CACHE_CREATED", "SERVER_ENTITY_FETCHED", "CLIENT_ATTACHED",
        "CLIENT_CACHE_CREATED", "SERVER_ENTITY_CREATED", "ENTITY_REGISTRY_AVAILABLE", "SERVER_CACHE_CREATED", "SERVER_ENTITY_FETCHED",
        "CLIENT_ATTACHED", "CLIENT_CACHE_CREATED", "SERVER_ENTITY_FETCHED", "CLIENT_ATTACHED", "CLIENT_CACHE_CREATED");

    String currentJson = toJson(notifs).toString();
    String actual = removeRandomValues(currentJson);
    String expected = readJson("notifications.json").toString();

    assertEquals(expected, actual);
  }

}
