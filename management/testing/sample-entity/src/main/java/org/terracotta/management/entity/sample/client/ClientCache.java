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
package org.terracotta.management.entity.sample.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.entity.sample.Cache;
import org.terracotta.statistics.StatisticsManager;
import org.terracotta.statistics.observer.OperationObserver;

import java.io.Closeable;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.terracotta.management.entity.sample.CacheOperationOutcomes.ClearOutcome;
import static org.terracotta.management.entity.sample.CacheOperationOutcomes.GetOutcome;
import static org.terracotta.management.entity.sample.CacheOperationOutcomes.PutOutcome;
import static org.terracotta.statistics.StatisticBuilder.operation;

/**
 * @author Mathieu Carbou
 */
public class ClientCache implements Cache, Closeable {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClientCache.class);

  private final String name;
  private final CacheEntity delegate;
  private final ConcurrentMap<String, String> data = new ConcurrentHashMap<>();

  private final OperationObserver<GetOutcome> getObserver = operation(GetOutcome.class).named("get").of(this).tag("cache").build();
  private final OperationObserver<PutOutcome> putObserver = operation(PutOutcome.class).named("put").of(this).tag("cache").build();
  private final OperationObserver<ClearOutcome> clearObserver = operation(ClearOutcome.class).named("clear").of(this).tag("cache").build();

  ClientCache(String name, CacheEntity delegate) {
    this.name = name;
    this.delegate = delegate;

    this.delegate.registerMessageListener(Serializable[].class, message -> {
      String cmd = (String) message[0];
      if ("remove".equals(cmd)) {
        remove((String) message[1], (String) message[2]);
      }
    });

    Map<String, Object> properties = new HashMap<>();
    properties.put("discriminator", "ClientCache");
    properties.put("name", name);
    StatisticsManager.createPassThroughStatistic(
        this,
        "size",
        new HashSet<>(Arrays.asList("ClientCache", "cache")),
        properties,
        this::size);
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
      delegate.put(key, value);
      putObserver.end(PutOutcome.SUCCESS);
    } catch (RuntimeException e) {
      putObserver.end(PutOutcome.FAILURE);
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
      if (v != null) {
        getObserver.end(GetOutcome.HIT);
        return v;
      }
      v = delegate.get(key);
      if (v != null) {
        String racer = data.putIfAbsent(key, v);
        getObserver.end(GetOutcome.HIT);
        return racer == null ? v : racer;
      }
      getObserver.end(GetOutcome.MISS);
      return null;
    } catch (RuntimeException e) {
      getObserver.end(GetOutcome.FAILURE);
      throw e;
    }
  }

  @Override
  public void remove(String key) {
    LOGGER.trace("[{}] remove({})", name, key);
    if (key == null) {
      throw new NullPointerException();
    }
    delegate.remove(key);
  }

  @Override
  public void clear() {
    LOGGER.trace("[{}] clear()", name);
    clearObserver.begin();
    try {
      data.clear();
      clearObserver.end(ClearOutcome.SUCCESS);
    } catch (RuntimeException e) {
      clearObserver.end(ClearOutcome.FAILURE);
    }
  }

  @Override
  public int size() {
    return data.size();
  }

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("ClientCache{");
    sb.append("name='").append(name).append('\'');
    sb.append('}');
    return sb.toString();
  }

  private void remove(String key, String val) {
    data.remove(key, val);
  }

  @Override
  public void close() {
    delegate.close();
  }
}
