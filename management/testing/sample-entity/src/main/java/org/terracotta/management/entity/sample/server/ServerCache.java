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

import org.terracotta.management.entity.sample.Cache;
import org.terracotta.management.entity.sample.CacheOperationOutcomes;
import org.terracotta.statistics.StatisticsManager;
import org.terracotta.statistics.observer.OperationObserver;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;

import static org.terracotta.statistics.StatisticBuilder.operation;

/**
 * @author Mathieu Carbou
 */
public class ServerCache implements Cache {

  private final ConcurrentMap<String, String> data = new ConcurrentHashMap<>();

  private final OperationObserver<CacheOperationOutcomes.GetOutcome> getObserver = operation(CacheOperationOutcomes.GetOutcome.class).named("get").of(this).tag("cluster").build();
  private final OperationObserver<CacheOperationOutcomes.PutOutcome> putObserver = operation(CacheOperationOutcomes.PutOutcome.class).named("put").of(this).tag("cluster").build();
  private final OperationObserver<CacheOperationOutcomes.ClearOutcome> clearObserver = operation(CacheOperationOutcomes.ClearOutcome.class).named("clear").of(this).tag("cluster").build();
  private final Random random = new Random();
  private final String name;

  private BiConsumer<String, String> evictionListener = (s, s2) -> {
  };

  ServerCache(String name) {
    this.name = name;
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

  void setEvictionListener(BiConsumer<String, String> evictionListener) {
    this.evictionListener = evictionListener;
  }

  @Override
  public void put(String key, String value) {
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
      evicted(key, old);
      putObserver.end(CacheOperationOutcomes.PutOutcome.PUT);
    } catch (RuntimeException e) {
      putObserver.end(CacheOperationOutcomes.PutOutcome.FAILURE);
      throw e;
    }
  }

  @Override
  public String get(String key) {
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
    if (key == null) {
      throw new NullPointerException();
    }
    String v = data.remove(key);
    evicted(key, v);
  }

  @Override
  public void clear() {
    clearObserver.begin();
    try {
      while (!data.isEmpty()) {
        Map.Entry<String, String> e = data.entrySet().iterator().next();
        if (data.remove(e.getKey(), e.getValue())) {
          evicted(e.getKey(), e.getValue());
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

  private void evicted(String key, String value) {
    if (value != null) {
      evictionListener.accept(key, value);
    }
  }

}
