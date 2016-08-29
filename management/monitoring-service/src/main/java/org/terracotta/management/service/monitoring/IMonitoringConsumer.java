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
package org.terracotta.management.service.monitoring;

import com.tc.classloader.CommonComponent;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.monitoring.PlatformConnectedClient;
import org.terracotta.monitoring.PlatformEntity;
import org.terracotta.monitoring.PlatformServer;
import org.terracotta.monitoring.ServerState;

import java.io.Closeable;
import java.io.Serializable;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author Mathieu Carbou
 */
@CommonComponent
public interface IMonitoringConsumer extends Closeable {

  /**
   * Get a read-only access to a monitoring tree of another consumer.
   *
   * @param consumerId The consumer id to whom the tree belongs to
   * @return The monitoring tree
   */
  Optional<MonitoringTree> getMonitoringTree(long consumerId);

  default Optional<MonitoringTree> getMonitoringTree(String entityType, String entityName) {
    return getConsumerId(entityType, entityName).flatMap(this::getMonitoringTree);
  }

  /**
   * @return The consumer ID of a specific entity
   */
  default Optional<Long> getConsumerId(String entityType, String entityName) {
    return getPlatformEntities()
        .filter(platformEntity -> platformEntity.typeName.equals(entityType) && platformEntity.name.equals(entityName))
        .findFirst()
        .map(platformEntity -> platformEntity.consumerID);
  }

  default Stream<Long> getConsumerIds(String entityType) {
    return getPlatformEntities()
        .filter(platformEntity -> platformEntity.typeName.equals(entityType))
        .map(platformEntity -> platformEntity.consumerID);
  }

  default Stream<MonitoringTree> getMonitoringTrees(String entityType) {
    return getConsumerIds(entityType)
        .map(this::getMonitoringTree)
        .filter(Optional::isPresent)
        .map(Optional::get);
  }

  Stream<PlatformServer> getPlatformServers();

  Stream<PlatformEntity> getPlatformEntities();

  Stream<PlatformConnectedClient> getPlatformConnectedClients();

  Stream<PlatformEntity> getFetchedEntities(PlatformConnectedClient platformConnectedClient);

  default Optional<PlatformServer> getPlatformServer(String serverName) {
    return getPlatformServers()
        .filter(platformServer -> platformServer.getServerName().equals(serverName))
        .findFirst();
  }

  Optional<ServerState> getServerState(String serverName);

  Optional<PlatformConnectedClient> getPlatformConnectedClient(ClientDescriptor clientDescriptor);

  /**
   * Get the buffer of notifications of the platform. They are ordered by creation time.
   * The stream returned is always the same and consumed messages cannot be consumed again.
   * This is a little like a 'read' method.
   * <p>
   * The stream ends when there is not more mutations to read.
   * <p>
   * There is also one stream per consumer. So mutations read by this consumer will still be available if other consumers are also reading.
   */
  default ReadOnlyBuffer<PlatformNotification> getOrCreatePlatformNotificationBuffer(int maxBufferSize) {
    return getOrCreateBestEffortBuffer("platform-notifications", maxBufferSize, PlatformNotification.class);
  }

  /**
   * Create a buffer where producers can push some data into. Not that push is a best effort so if the buffer is full, some data might be discarded without any notice.
   *
   * @param category The category name of this producer, used in {@link IMonitoringProducer#pushBestEffortsData(String, Serializable)}
   * @param type     The class of data for casting
   * @param <V>      The type of data expected to receive in this buffer
   * @return a read-only buffer
   */
  <V extends Serializable> ReadOnlyBuffer<V> getOrCreateBestEffortBuffer(String category, int maxBufferSize, Class<V> type);

  /**
   * closes this consumer
   */
  void close();
}
