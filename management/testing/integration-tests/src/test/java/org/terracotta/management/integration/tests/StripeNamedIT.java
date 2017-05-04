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
import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionFactory;
import org.terracotta.connection.ConnectionPropertyNames;
import org.terracotta.management.entity.nms.NmsConfig;
import org.terracotta.management.entity.nms.client.DefaultNmsService;
import org.terracotta.management.entity.nms.client.NmsEntity;
import org.terracotta.management.entity.nms.client.NmsEntityFactory;
import org.terracotta.management.entity.nms.client.NmsService;
import org.terracotta.management.model.cluster.Client;
import org.terracotta.management.model.cluster.Stripe;
import org.terracotta.management.model.message.Message;
import org.terracotta.management.model.notification.ContextualNotification;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author Mathieu Carbou
 */
public class StripeNamedIT extends AbstractSingleTest {

  @Test
  public void stripe_can_be_named() throws Exception {
    // connects to server
    Properties properties = new Properties();
    properties.setProperty(ConnectionPropertyNames.CONNECTION_NAME, getClass().getSimpleName() + "-2"); // to have a different name than the client created in the super class
    properties.setProperty(ConnectionPropertyNames.CONNECTION_TIMEOUT, "5000");
    Connection managementConnection = ConnectionFactory.connect(voltron.getConnectionURI(), properties);

    // create a NMS Entity
    NmsEntityFactory nmsEntityFactory = new NmsEntityFactory(managementConnection, getClass().getSimpleName() + "-2");
    NmsEntity nmsEntity = nmsEntityFactory.retrieveOrCreate(new NmsConfig()
        .setStripeName("MY_SUPER_STRIPE"));
    NmsService nmsService = new DefaultNmsService(nmsEntity);
    nmsService.setOperationTimeout(60, TimeUnit.SECONDS);

    String currentTopo = toJson(nmsService.readTopology().toMap()).toString();
    String actual = removeRandomValues(currentTopo);
    String expected = readJson("topology-renamed.json").toString();

    assertEquals(expected, actual);

    // clear previous notifs
    nmsService.readMessages();

    // create a new cache and server entity
    getCaches("orders");

    List<Message> messages = nmsService.waitForMessage(message -> message.getType().equals("NOTIFICATION")
        && message.unwrap(ContextualNotification.class)
        .stream()
        .anyMatch(n -> n.getType().equals("CLIENT_CACHE_CREATED") && "orders".equals(n.getContext().get("cacheName"))));

    List<ContextualNotification> notifications = messages
        .stream()
        .filter(message -> message.getType().equals("NOTIFICATION"))
        .flatMap(message -> message.unwrap(ContextualNotification.class).stream())
        .collect(Collectors.toList());

    assertThat(notifications.size(), is(not(equalTo(0))));
    for (ContextualNotification notification : notifications) {
      if(!notification.getContext().contains(Client.KEY)) {
        assertThat(notification.getContext().get(Stripe.KEY), equalTo("MY_SUPER_STRIPE"));  
      }
    }
  }

}
