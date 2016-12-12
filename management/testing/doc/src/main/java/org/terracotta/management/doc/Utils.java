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
package org.terracotta.management.doc;

import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionException;
import org.terracotta.connection.ConnectionFactory;
import org.terracotta.connection.ConnectionPropertyNames;
import org.terracotta.exception.EntityConfigurationException;
import org.terracotta.management.entity.tms.TmsAgentConfig;
import org.terracotta.management.entity.tms.client.TmsAgentEntity;
import org.terracotta.management.entity.tms.client.TmsAgentEntityFactory;
import org.terracotta.management.entity.tms.client.TmsAgentService;
import org.terracotta.management.registry.collect.StatisticConfiguration;

import java.net.URI;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * @author Mathieu Carbou
 */
class Utils {

  static Connection createConnection(String name, String uri) throws ConnectionException {
    Properties properties = new Properties();
    properties.setProperty(ConnectionPropertyNames.CONNECTION_NAME, name);
    properties.setProperty(ConnectionPropertyNames.CONNECTION_TIMEOUT, "10000");
    return ConnectionFactory.connect(URI.create(uri), properties);
  }

  static TmsAgentService createTmsAgentService(Connection connection, String entityName) throws EntityConfigurationException {
    TmsAgentEntityFactory factory = new TmsAgentEntityFactory(connection, entityName);
    TmsAgentEntity tmsAgentEntity = factory.retrieveOrCreate(new TmsAgentConfig()
        .setMaximumUnreadMessages(1024 * 1024)
        .setStatisticConfiguration(new StatisticConfiguration()
            .setAverageWindowDuration(1, TimeUnit.MINUTES)
            .setHistorySize(100)
            .setHistoryInterval(1, TimeUnit.SECONDS)
            .setTimeToDisable(5, TimeUnit.SECONDS)));
    return new TmsAgentService(tmsAgentEntity);
  }

}
