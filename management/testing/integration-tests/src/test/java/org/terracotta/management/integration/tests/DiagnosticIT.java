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

import com.terracotta.diagnostic.Diagnostics;
import org.hamcrest.core.StringContains;
import org.junit.Assert;
import org.junit.Test;
import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionFactory;
import org.terracotta.connection.ConnectionPropertyNames;
import org.terracotta.connection.entity.EntityRef;

import java.net.URI;
import java.util.Properties;

import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Mathieu Carbou
 */
public class DiagnosticIT extends AbstractSingleTest {

  private static final String PROP_REQUEST_TIMEOUT = "request.timeout";
  private static final String PROP_REQUEST_TIMEOUTMESSAGE = "request.timeoutMessage";

  @Test
  public void cluster_state_dump() throws Exception {
    put(0, "pets", "pet1", "Cubitus");

    Properties properties = new Properties();
    properties.setProperty(ConnectionPropertyNames.CONNECTION_TIMEOUT, String.valueOf("5000"));
    properties.setProperty(ConnectionPropertyNames.CONNECTION_NAME, "diagnostic");
    properties.setProperty(PROP_REQUEST_TIMEOUT, "5000");
    properties.setProperty(PROP_REQUEST_TIMEOUTMESSAGE, "5000");
    URI uri = URI.create("diagnostic://" + voltron.getConnectionURI().getAuthority());
    try (Connection connection = ConnectionFactory.connect(uri, properties)) {
      EntityRef<Diagnostics, Object, Void> ref = connection.getEntityRef(Diagnostics.class, 1, "root");
      Diagnostics diagnostics = ref.fetchEntity(null);

      //TODO: improve these assertions
      // once https://github.com/Terracotta-OSS/terracotta-core/issues/613 and https://github.com/Terracotta-OSS/terracotta-core/pull/601 will be fixed 
      // and once the state dump format will be improved.
      String dump = diagnostics.getClusterState();
      
      // monitoring service provider
      assertThat(dump, containsString("cluster="));

      // ActiveNmsServerEntity / PassiveNmsServerEntity
      assertThat(dump, containsString("consumerId="));
      assertThat(dump, containsString("stripeName="));
      assertThat(dump, containsString("messageQueueSize="));
      
      // OffHeapResourcesProvider
      assertThat(dump, containsString("capacity="));
      assertThat(dump, containsString("available="));
      
      // ActiveCacheServerEntity / ActiveCacheServerEntity
      assertThat(dump, containsString("cacheName="));
      assertThat(dump, containsString("cacheSize="));
      
      // MapProvider
      assertThat(dump, containsString("caches="));
      
      // Common on all active entities
      assertThat(dump, containsString("instance="));
      assertThat(dump, containsString("clientCount="));
      assertThat(dump, containsString("clients="));
    }
  }

}
