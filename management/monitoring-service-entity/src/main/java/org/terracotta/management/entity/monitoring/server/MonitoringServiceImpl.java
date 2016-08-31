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
package org.terracotta.management.entity.monitoring.server;

import org.terracotta.management.entity.monitoring.MonitoringService;
import org.terracotta.management.service.monitoring.IMonitoringConsumer;
import org.terracotta.management.service.monitoring.ReadOnlyBuffer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Mathieu Carbou
 */
class MonitoringServiceImpl implements MonitoringService {

  private final IMonitoringConsumer consumer;
  private final ConcurrentMap<String, ReadOnlyBuffer<?>> buffers = new ConcurrentHashMap<>();

  MonitoringServiceImpl(IMonitoringConsumer consumer) {
    this.consumer = consumer;
  }

  @Override
  public long getConsumerId(String entityType, String entityName) throws NoSuchElementException {
    return consumer.getConsumerId(entityType, entityName).get();
  }

  @Override
  public Serializable getValueForNode(long consumerId, String[] path) {
    return getValueForNode(consumerId, path, Serializable.class);
  }

  @Override
  public <T extends Serializable> T getValueForNode(long consumerId, String[] parents, String nodeName, Class<T> type) {
    return getValueForNode(consumerId, concat(parents, nodeName), type);
  }

  @Override
  public Serializable getValueForNode(long consumerId, String[] parents, String nodeName) {
    return getValueForNode(consumerId, concat(parents, nodeName), Serializable.class);
  }

  @Override
  public <T extends Serializable> T getValueForNode(long consumerId, String[] path, Class<T> type) {
    return consumer.getMonitoringTree(consumerId)
        .flatMap(tree -> tree.getValueForNode(path, type))
        .orElse(null);
  }

  @Override
  public Collection<String> getChildNamesForNode(long consumerId, String[] parent, String nodeName) {
    return getChildNamesForNode(consumerId, concat(parent, nodeName));
  }

  @Override
  public Collection<String> getChildNamesForNode(long consumerId, String... path) {
    return consumer.getMonitoringTree(consumerId)
        .flatMap(tree -> tree.getChildNamesForNode(path))
        .map(ArrayList::new)
        .orElse(null);
  }

  @Override
  public Map<String, Serializable> getChildValuesForNode(long consumerId, String[] parent, String nodeName) {
    return getChildValuesForNode(consumerId, concat(parent, nodeName));
  }

  @Override
  public Map<String, Serializable> getChildValuesForNode(long consumerId, String... path) {
    return consumer.getMonitoringTree(consumerId)
        .flatMap(tree -> tree.getChildValuesForNode(path))
        .map(HashMap::new)
        .orElse(null);
  }

  @Override
  public void createBestEffortBuffer(String name, int size, Class<? extends Serializable> type) {
    buffers.putIfAbsent(name, consumer.getOrCreateBestEffortBuffer(name, size, type));
  }

  @Override
  public <T extends Serializable> T readBuffer(String name, Class<T> type) {
    return Optional.ofNullable(buffers.get(name)).map(ReadOnlyBuffer::read).map(type::cast).orElse(null);
  }

  @Override
  public void clearBuffer(String name) {
    Optional.ofNullable(buffers.get(name)).ifPresent(ReadOnlyBuffer::clear);
  }


  private static String[] concat(String[] parents, String name) {
    if (parents == null) {
      parents = new String[0];
    }
    String[] fullPath = Arrays.copyOf(parents, parents.length + 1);
    fullPath[parents.length] = name;
    return fullPath;
  }

}
