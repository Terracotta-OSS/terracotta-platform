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

import org.terracotta.entity.ClientDescriptor;
import org.terracotta.monitoring.PlatformClientFetchedEntity;
import org.terracotta.monitoring.PlatformConnectedClient;
import org.terracotta.monitoring.PlatformEntity;
import org.terracotta.monitoring.PlatformServer;
import org.terracotta.monitoring.ServerState;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.terracotta.monitoring.PlatformMonitoringConstants.CLIENTS_PATH;
import static org.terracotta.monitoring.PlatformMonitoringConstants.CLIENTS_ROOT_NAME;
import static org.terracotta.monitoring.PlatformMonitoringConstants.ENTITIES_PATH;
import static org.terracotta.monitoring.PlatformMonitoringConstants.ENTITIES_ROOT_NAME;
import static org.terracotta.monitoring.PlatformMonitoringConstants.FETCHED_PATH;
import static org.terracotta.monitoring.PlatformMonitoringConstants.PLATFORM_ROOT_NAME;
import static org.terracotta.monitoring.PlatformMonitoringConstants.SERVERS_PATH;
import static org.terracotta.monitoring.PlatformMonitoringConstants.SERVERS_ROOT_NAME;

/**
 * @author Mathieu Carbou
 */
class DefaultMonitoringConsumer implements IMonitoringConsumer {

  private final long consumerId;
  private final Map<Long, DefaultMonitoringConsumer> consumers;
  private final Function<Long, Optional<MonitoringTree>> treeProvider;
  private final Map<String, ReadWriteBuffer<Serializable>> buffers = new ConcurrentHashMap<>();

  DefaultMonitoringConsumer(long consumerId, Map<Long, DefaultMonitoringConsumer> consumers, Function<Long, Optional<MonitoringTree>> treeProvider) {
    this.consumerId = consumerId;
    this.consumers = consumers;
    this.treeProvider = treeProvider;
  }

  @Override
  public <V extends Serializable> ReadOnlyBuffer<V> getOrCreateBestEffortBuffer(String category, int maxBufferSize, Class<V> type) {
    if (VoltronMonitoringService.PLATFORM_CATEGORY.equals(category) && PlatformNotification.class != type) {
      throw new IllegalArgumentException("Protected buffer name: " + VoltronMonitoringService.PLATFORM_CATEGORY);
    }
    return new TypedReadWriteBuffer<>(buffers.computeIfAbsent(category, s -> new RingBuffer<>(maxBufferSize)), type);
  }

  @Override
  public void close() {
    consumers.remove(consumerId);
    buffers.clear();
  }

  @Override
  public String toString() {
    return "MonitoringConsumer{" + "consumerId=" + consumerId + '}';
  }

  @Override
  public Optional<MonitoringTree> getMonitoringTree(long consumerId) {
    return treeProvider.apply(consumerId);
  }

  @Override
  public Optional<PlatformConnectedClient> getPlatformConnectedClient(ClientDescriptor clientDescriptor) {
    MonitoringTree tree = getPlatformMonitoringTree();
    return tree.getChildValuesForNode(FETCHED_PATH)
        .map(Map::values)
        .orElse(Collections.emptyList())
        .stream()
        .map(PlatformClientFetchedEntity.class::cast)
        .filter(fetch -> clientDescriptor.equals(fetch.clientDescriptor))
        .findFirst()
        .flatMap(fetch -> tree.getValueForNode(Utils.array(PLATFORM_ROOT_NAME, CLIENTS_ROOT_NAME, fetch.clientIdentifier), PlatformConnectedClient.class));
  }

  @Override
  public Stream<PlatformServer> getPlatformServers() {
    return getPlatformMonitoringTree().getChildValuesForNode(SERVERS_PATH)
        .map(Map::values)
        .orElse(Collections.emptyList())
        .stream()
        .map(PlatformServer.class::cast);
  }

  @Override
  public Stream<PlatformEntity> getPlatformEntities() {
    return getPlatformMonitoringTree().getChildValuesForNode(ENTITIES_PATH)
        .map(Map::values)
        .orElse(Collections.emptyList())
        .stream()
        .map(PlatformEntity.class::cast);
  }

  @Override
  public Stream<PlatformConnectedClient> getPlatformConnectedClients() {
    return getPlatformMonitoringTree().getChildValuesForNode(CLIENTS_PATH)
        .map(Map::values)
        .orElse(Collections.emptyList())
        .stream()
        .map(PlatformConnectedClient.class::cast);
  }

  @Override
  public Stream<PlatformEntity> getFetchedEntities(PlatformConnectedClient platformConnectedClient) {
    MonitoringTree tree = getPlatformMonitoringTree();
    return tree.getChildValuesForNode(CLIENTS_PATH)
        .map(Map::entrySet)
        .orElse(Collections.emptySet())
        .stream()
        .filter(entry -> entry.getValue().equals(platformConnectedClient))
        .findFirst()
        .map(clientEntry -> tree.getChildValuesForNode(FETCHED_PATH)
            .map(Map::values)
            .orElse(Collections.emptyList())
            .stream()
            .map(PlatformClientFetchedEntity.class::cast)
            .filter(fetch -> clientEntry.getKey().equals(fetch.clientIdentifier))
            .flatMap(fetch -> tree.getValueForNode(Utils.array(PLATFORM_ROOT_NAME, ENTITIES_ROOT_NAME, fetch.entityIdentifier), PlatformEntity.class)
                .map(Stream::of)
                .orElse(Stream.empty())))
        .orElse(Stream.empty());
  }

  @Override
  public Optional<ServerState> getServerState(String serverName) {
    MonitoringTree tree = getPlatformMonitoringTree();
    return tree.getChildValuesForNode(SERVERS_PATH)
        .map(Map::entrySet)
        .orElse(Collections.emptySet())
        .stream()
        .filter(entry -> PlatformServer.class.cast(entry.getValue()).getServerName().equals(serverName))
        .findFirst()
        .flatMap(entry -> tree.getValueForNode(Utils.array(PLATFORM_ROOT_NAME, SERVERS_ROOT_NAME, entry.getKey(), "state"), ServerState.class));
  }

  void push(String category, Serializable data) {
    ReadWriteBuffer<Serializable> buffer = buffers.get(category);
    if (buffer != null) {
      buffer.put(data);
    }
  }

  private MonitoringTree getPlatformMonitoringTree() {
    return getMonitoringTree(VoltronMonitoringService.PLATFORM_CONSUMERID)
        .orElseThrow(() -> new NoSuchElementException("Platform MonitoringTree not found"));
  }

}
