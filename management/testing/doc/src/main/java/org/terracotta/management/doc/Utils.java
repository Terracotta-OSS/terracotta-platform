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
import org.terracotta.management.entity.nms.NmsConfig;
import org.terracotta.management.entity.nms.client.NmsEntity;
import org.terracotta.management.entity.nms.client.NmsEntityFactory;
import org.terracotta.management.entity.nms.client.DefaultNmsService;
import org.terracotta.management.entity.nms.client.NmsService;

import java.net.URI;
import java.util.Properties;

/**
 * @author Mathieu Carbou
 */
class Utils {

  static Connection createConnection(String name, String uri) throws ConnectionException {
    Properties properties = new Properties();
    properties.setProperty(ConnectionPropertyNames.CONNECTION_NAME, name);
    properties.setProperty(ConnectionPropertyNames.CONNECTION_TIMEOUT, "5000");
    return ConnectionFactory.connect(URI.create(uri), properties);
  }

  static NmsService createNmsService(Connection connection, String entityName) throws EntityConfigurationException {
    NmsEntityFactory factory = new NmsEntityFactory(connection, entityName);
    NmsEntity nmsEntity = factory.retrieveOrCreate(new NmsConfig());
    return new DefaultNmsService(nmsEntity);
  }

}
