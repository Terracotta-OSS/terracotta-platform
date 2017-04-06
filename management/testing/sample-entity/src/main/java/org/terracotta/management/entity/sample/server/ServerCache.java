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
package org.terracotta.management.entity.sample.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.entity.sample.Cache;
import org.terracotta.management.entity.sample.CacheOperationOutcomes;
import org.terracotta.statistics.StatisticsManager;
import org.terracotta.statistics.observer.OperationObserver;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.terracotta.statistics.StatisticBuilder.operation;

/**
 * @author Mathieu Carbou
 */
public class ServerCache implements Cache {

  private static final Logger LOGGER = LoggerFactory.getLogger(ServerCache.class);

  private final Map<String, String> data;

  private final OperationObserver<CacheOperationOutcomes.GetOutcome> getObserver = operation(CacheOperationOutcomes.GetOutcome.class).named("get").of(this).tag("cluster").build();
  private final OperationObserver<CacheOperationOutcomes.PutOutcome> putObserver = operation(CacheOperationOutcomes.PutOutcome.class).named("put").of(this).tag("cluster").build();
  private final OperationObserver<CacheOperationOutcomes.ClearOutcome> clearObserver = operation(CacheOperationOutcomes.ClearOutcome.class).named("clear").of(this).tag("cluster").build();
  private final Random random = new Random();
  private final String name;

  private Collection<Listener> listeners = new CopyOnWriteArrayList<>();

  ServerCache(String name, Map<String, String> data) {
    this.name = name;
    this.data = data;
    // add a passthrough stat for size
    Map<String, Object> properties = new HashMap<>();
    properties.put("discriminator", "ServerCache");
    properties.put("name", name);
    StatisticsManager.createPassThroughStatistic(
        this,
        "size",
        new HashSet<>(Arrays.asList("ServerCache", "cluster")),
        properties,
        this::size);
  }

  void removeListener(Listener listener) {
    listeners.remove(listener);
  }

  void addListener(Listener listener) {
    listeners.add(listener);
  }

  @Override
  public void put(String key, String value) {
    LOGGER.trace("[{}] put({}, {})", name, key, value);
    if (key == null) {
      throw new NullPointerException();
    }
    if (value == null) {
      throw new NullPointerException();
    }
    putObserver.begin();
    try {
      String old = data.put(key, value);
      simulateLatency();
      removed(key, old);
      putObserver.end(CacheOperationOutcomes.PutOutcome.SUCCESS);
    } catch (RuntimeException e) {
      putObserver.end(CacheOperationOutcomes.PutOutcome.FAILURE);
      throw e;
    }
  }

  @Override
  public String get(String key) {
    LOGGER.trace("[{}] get({})", name, key);
    if (key == null) {
      throw new NullPointerException();
    }
    getObserver.begin();
    try {
      String v = data.get(key);
      simulateLatency();
      getObserver.end(v == null ? CacheOperationOutcomes.GetOutcome.MISS : CacheOperationOutcomes.GetOutcome.HIT);
      return v;
    } catch (RuntimeException e) {
      getObserver.end(CacheOperationOutcomes.GetOutcome.FAILURE);
      throw e;
    }
  }

  @Override
  public void remove(String key) {
    LOGGER.trace("[{}] remove({})", name, key);
    if (key == null) {
      throw new NullPointerException();
    }
    String v = data.remove(key);
    removed(key, v);
  }

  @Override
  public void clear() {
    LOGGER.trace("[{}] clear()", name);
    clearObserver.begin();
    try {
      while (!data.isEmpty()) {
        Map.Entry<String, String> e = data.entrySet().iterator().next();
        if (data.remove(e.getKey(), e.getValue())) {
          removed(e.getKey(), e.getValue());
        }
      }
      simulateLatency();
      clearObserver.end(CacheOperationOutcomes.ClearOutcome.SUCCESS);
    } catch (RuntimeException e) {
      clearObserver.end(CacheOperationOutcomes.ClearOutcome.FAILURE);
    }
  }

  @Override
  public int size() {
    return data.size();
  }

  public String getName() {
    return name;
  }

  private void simulateLatency() {
    try {
      Thread.sleep(100 + random.nextInt(400));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private void removed(String key, String value) {
    if (value != null) {
      for (Listener l : listeners) {
        l.onRemove(key, value);
      }
    }
  }

  Map<String, String> getData() {
    return new HashMap<>(data);
  }

  interface Listener {
    void onRemove(String key, String val);
  }
}
