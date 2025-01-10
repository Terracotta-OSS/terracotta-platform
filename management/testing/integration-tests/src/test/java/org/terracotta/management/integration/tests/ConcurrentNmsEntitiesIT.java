/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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
import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionException;
import org.terracotta.connection.ConnectionFactory;
import org.terracotta.connection.ConnectionPropertyNames;
import org.terracotta.exception.EntityConfigurationException;
import org.terracotta.management.entity.nms.NmsConfig;
import org.terracotta.management.entity.nms.client.DefaultNmsService;
import org.terracotta.management.entity.nms.client.NmsEntity;
import org.terracotta.management.entity.nms.client.NmsEntityFactory;
import org.terracotta.management.entity.nms.client.NmsService;
import org.terracotta.management.model.cluster.ServerEntity;
import org.terracotta.management.model.message.Message;
import org.terracotta.management.model.stats.ContextualStatistics;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Mathieu Carbou
 */
public class ConcurrentNmsEntitiesIT extends AbstractSingleTest {

  String name1 = getClass().getSimpleName() + "-1";
  String name2 = getClass().getSimpleName() + "-2";

  Connection connection1;
  Connection connection2;

  NmsService nmsService1;
  NmsService nmsService2;

  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();

    connection1 = createConnection(name1);
    connection2 = createConnection(name2);
    nmsService1 = createNmsService(connection1, name1);
    nmsService2 = createNmsService(connection2, name2);
  }

  @Override
  public void tearDown() throws Exception {
    try {
      connection1.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    try {
      connection2.close();
    } catch (IOException e) {
      e.printStackTrace();
    }

    super.tearDown();
  }

  @Test
  public void test_stats_do_not_overlap() throws Exception {
    triggerServerStatComputation(nmsService1, name1, 1, TimeUnit.SECONDS);
    triggerServerStatComputation(nmsService2, name2, 1, TimeUnit.SECONDS);

    Thread.sleep(10_000);
    List<Message> messages1 = nmsService1.readMessages();
    List<Message> messages2 = nmsService2.readMessages();

    System.out.println("NMS1");
    System.out.println(" - " + messages1.stream().map(Object::toString).collect(Collectors.joining("\n - ")));
    System.out.println("NMS2");
    System.out.println(" - " + messages2.stream().map(Object::toString).collect(Collectors.joining("\n - ")));

    Collection<String> entityNames1 = entityNames(messages1);
    Collection<String> entityNames2 = entityNames(messages2);

    System.out.println(entityNames1);
    System.out.println(entityNames2);

    assertThat(entityNames1.isEmpty(), is(false));
    assertThat(entityNames2.isEmpty(), is(false));

    assertThat(entityNames1, hasItems(name1, "pet-clinic/clients", "pet-clinic/pets"));
    assertThat(entityNames2, hasItems(name2, "pet-clinic/clients", "pet-clinic/pets"));

    assertThat(entityNames1, not(hasItem(name2)));
    assertThat(entityNames2, not(hasItem(name1)));
  }

  private Collection<String> entityNames(List<Message> messages) {
    return messages.stream()
        .filter(message -> message.getType().equals("STATISTICS"))
        .flatMap(message -> message.unwrap(ContextualStatistics.class).stream())
        .map(ContextualStatistics::getContext)
        .map(context -> context.get(ServerEntity.NAME_KEY))
        .collect(Collectors.toSet());
  }

  private NmsService createNmsService(Connection connection, String name) throws EntityConfigurationException {
    NmsEntityFactory nmsEntityFactory = new NmsEntityFactory(connection, name);
    NmsEntity nmsEntity = nmsEntityFactory.retrieveOrCreate(new NmsConfig());
    DefaultNmsService nmsService = new DefaultNmsService(nmsEntity);
    nmsService.setOperationTimeout(60, TimeUnit.SECONDS);
    return nmsService;
  }

  private Connection createConnection(String name) throws ConnectionException {
    Properties properties = new Properties();
    properties.setProperty(ConnectionPropertyNames.CONNECTION_NAME, name);
    properties.setProperty(ConnectionPropertyNames.CONNECTION_TIMEOUT, "5000");
    return ConnectionFactory.connect(voltron.getConnectionURI(), properties);
  }

}
