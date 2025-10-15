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
package org.terracotta.stats.entity.common;

import org.junit.Test;
import org.terracotta.connection.entity.Entity;
import org.terracotta.voltron.proxy.MessageListener;
import org.terracotta.voltron.proxy.client.EndpointListener;
import org.terracotta.voltron.proxy.client.EndpointListenerAware;
import org.terracotta.voltron.proxy.client.ServerMessageAware;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import static org.junit.Assert.assertTrue;

public class StatsEntityTest {

  @Test
  public void testStatsEntityCanBeImplemented() {

    StatsEntity statsEntity = new StatsEntity() {

      @Override
      public Future<Map<String, Object>> collectCacheStatistics() {
        return CompletableFuture.completedFuture(Collections.emptyMap());
      }

      @Override
      public Future<Map<String, Object>> collectDatasetStatistics() {
        return CompletableFuture.completedFuture(Collections.emptyMap());
      }

      @Override
      public Future<Map<String, Object>> collectServerStatistics() {
        return CompletableFuture.completedFuture(Collections.emptyMap());
      }

      @Override
      public void close() {
        // No-op for test implementation
      }
      @Override
      public <T> void registerMessageListener(Class<T> type, MessageListener<T> listener) {
        // No-op for test implementation
      }

      @Override
      public void setEndpointListener(EndpointListener endpointListener) {
          // No-op for test implementation
      }
    };

    // Verify that the implementation extends the expected interfaces
    assertTrue("StatsEntity should extend Stats", statsEntity instanceof Stats);
    assertTrue("StatsEntity should extend Entity", statsEntity instanceof Entity);
    assertTrue("StatsEntity should extend ServerMessageAware", statsEntity instanceof ServerMessageAware);
    assertTrue("StatsEntity should extend EndpointListenerAware", statsEntity instanceof EndpointListenerAware);
  }
}
