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
import org.terracotta.management.entity.tms.TmsAgentConfig;
import org.terracotta.management.entity.tms.client.DefaultTmsAgentService;
import org.terracotta.management.entity.tms.client.TmsAgentEntity;
import org.terracotta.management.entity.tms.client.TmsAgentEntityFactory;
import org.terracotta.management.entity.tms.client.TmsAgentService;
import org.terracotta.management.model.cluster.Stripe;
import org.terracotta.management.model.notification.ContextualNotification;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
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

    // create a tms entity
    TmsAgentEntityFactory tmsAgentEntityFactory = new TmsAgentEntityFactory(managementConnection, getClass().getSimpleName() + "-2");
    TmsAgentEntity tmsAgentEntity = tmsAgentEntityFactory.retrieveOrCreate(new TmsAgentConfig()
        .setMaximumUnreadMessages(1024 * 1024)
        .setStripeName("MY_SUPER_STRIPE"));
    TmsAgentService tmsAgentService = new DefaultTmsAgentService(tmsAgentEntity);
    tmsAgentService.setOperationTimeout(60, TimeUnit.SECONDS);

    String currentTopo = toJson(tmsAgentService.readTopology().toMap()).toString();
    String actual = removeRandomValues(currentTopo);
    String expected = readJson("topology-renamed.json").toString();

    assertEquals(expected, actual);

    // clear previous notifs
    tmsAgentService.readMessages();

    // create a new cache and serevr entity
    getCaches("orders");

    List<ContextualNotification> notifications = tmsAgentService.readMessages()
        .stream()
        .filter(message -> message.getType().equals("NOTIFICATION"))
        .flatMap(message -> message.unwrap(ContextualNotification.class).stream())
        .filter(contextualNotification -> contextualNotification.getContext().contains(Stripe.KEY))
        .collect(Collectors.toList());

    assertThat(notifications.isEmpty(), is(false));
    for (ContextualNotification notification : notifications) {
      assertThat(notification.getContext().get(Stripe.KEY), equalTo("MY_SUPER_STRIPE"));
    }
  }

}
