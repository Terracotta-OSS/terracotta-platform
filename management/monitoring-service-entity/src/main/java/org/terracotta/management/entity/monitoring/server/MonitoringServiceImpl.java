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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Mathieu Carbou
 */
class MonitoringServiceImpl implements MonitoringService {

  private final IMonitoringConsumer monitoringConsumer;
  private final ConcurrentMap<String, ReadOnlyBuffer<?>> buffers = new ConcurrentHashMap<>();

  MonitoringServiceImpl(IMonitoringConsumer monitoringConsumer) {
    this.monitoringConsumer = monitoringConsumer;
  }

  @Override
  public Collection<String> getChildNamesForNode(String[] parent, String nodeName) {
    return monitoringConsumer.getChildNamesForNode(parent, nodeName).map(ArrayList::new).orElse(null);
  }

  @Override
  public Object getValueForNode(String[] path) {
    return getValueForNode(path, Object.class);
  }

  @Override
  public <T> T getValueForNode(String[] parents, String nodeName, Class<T> type) {
    return getValueForNode(concat(parents, nodeName), type);
  }

  @Override
  public Object getValueForNode(String[] parents, String nodeName) {
    return getValueForNode(concat(parents, nodeName), Object.class);
  }

  @Override
  public <T> T getValueForNode(String[] path, Class<T> type) {
    return monitoringConsumer.getValueForNode(path, type).orElse(null);
  }

  @Override
  public Collection<String> getChildNamesForNode(String... path) {
    return monitoringConsumer.getChildNamesForNode(path).map(ArrayList::new).orElse(null);
  }

  @Override
  public Map<String, Object> getChildValuesForNode(String... path) {
    return monitoringConsumer.getChildValuesForNode(path).map(HashMap::new).orElse(null);
  }

  @Override
  public Map<String, Object> getChildValuesForNode(String[] parent, String nodeName) {
    return monitoringConsumer.getChildValuesForNode(parent, nodeName).map(HashMap::new).orElse(null);
  }

  @Override
  public void createBestEffortBuffer(String name, int size, Class<?> type) {
    buffers.putIfAbsent(name, monitoringConsumer.getOrCreateBestEffortBuffer(name, size, type));
  }

  @Override
  public <T> T readBuffer(String name, Class<T> type) {
    return Optional.ofNullable(buffers.get(name)).map(ReadOnlyBuffer::read).map(type::cast).orElse(null);
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
