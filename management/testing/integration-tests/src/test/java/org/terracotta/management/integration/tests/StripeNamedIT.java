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

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author Mathieu Carbou
 */
public class StripeNamedIT extends AbstractSingleTest {

  @Test
  public void stripe_can_be_named() throws Exception {
    // connects to server
    Properties properties = new Properties();
    properties.setProperty(ConnectionPropertyNames.CONNECTION_NAME, getClass().getSimpleName());
    properties.setProperty(ConnectionPropertyNames.CONNECTION_TIMEOUT, "5000");
    Connection managementConnection = ConnectionFactory.connect(voltron.getConnectionURI(), properties);

    // create a tms entity
    TmsAgentEntityFactory tmsAgentEntityFactory = new TmsAgentEntityFactory(managementConnection, getClass().getSimpleName() + "-2");
    TmsAgentEntity tmsAgentEntity = tmsAgentEntityFactory.retrieveOrCreate(new TmsAgentConfig()
        .setMaximumUnreadMessages(1024 * 1024)
        .setStripeName("MY SUPER STRIPE"));
    TmsAgentService tmsAgentService = new DefaultTmsAgentService(tmsAgentEntity);
    tmsAgentService.setOperationTimeout(60, TimeUnit.SECONDS);

    assertThat(tmsAgentService.readTopology().getSingleStripe().getName(), equalTo("MY SUPER STRIPE"));
  }

}
