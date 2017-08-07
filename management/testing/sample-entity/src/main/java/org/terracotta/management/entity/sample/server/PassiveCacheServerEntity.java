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
import org.terracotta.entity.ServiceException;
import org.terracotta.entity.ServiceRegistry;
import org.terracotta.entity.StateDumpCollector;
import org.terracotta.management.entity.sample.Cache;
import org.terracotta.management.entity.sample.server.management.Management;
import org.terracotta.voltron.proxy.server.PassiveProxiedServerEntity;

import java.util.Map;

/**
 * @author Mathieu Carbou
 */
class PassiveCacheServerEntity extends PassiveProxiedServerEntity implements Cache, CacheSync {

  private static final Logger LOGGER = LoggerFactory.getLogger(PassiveCacheServerEntity.class);

  private final Management management;
  private final ServiceRegistry registry;
  private final ServerCache cache;

  PassiveCacheServerEntity(ServerCache cache, Management management, ServiceRegistry registry) {
    this.cache = cache;
    this.management = management;
    this.registry = registry;
  }

  @Override
  public void createNew() {
    super.createNew();
    
    LOGGER.trace("[{}] createNew()", cache.getName());
    management.init();
    management.serverCacheCreated(cache);
  }

  @Override
  public void destroy() {
    LOGGER.trace("[{}] destroy()", cache.getName());
    management.serverCacheDestroyed(cache);
    management.close();

    // this is just a hack to tell the service to release some offheap
    try {
      registry.getService(new MapRelease());
    } catch (ServiceException e) {
      e.printStackTrace();
    }
    
    super.destroy();
  }

  @Override
  public void startSyncEntity() {
    LOGGER.trace("[{}] startSyncEntity()", cache.getName());
    management.startSync(cache);
  }

  @Override
  public void endSyncEntity() {
    LOGGER.trace("[{}] endSyncEntity()", cache.getName());
    management.endSync(cache);
  }

  @Override
  public void startSyncConcurrencyKey(int concurrencyKey) {
    LOGGER.trace("[{}] startSyncConcurrencyKey({})", cache.getName(), concurrencyKey);
  }

  @Override
  public void endSyncConcurrencyKey(int concurrencyKey) {
    LOGGER.trace("[{}] endSyncConcurrencyKey({})", cache.getName(), concurrencyKey);
  }


  @Override
  public void syncCacheDataInPassives(Map<String, String> data) {
    LOGGER.trace("[{}] syncCacheDataInPassives({})", cache.getName(), data.size());
    clear();
    for (Map.Entry<String, String> entry : data.entrySet()) {
      put(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public void put(String key, String value) {cache.put(key, value);}

  @Override
  public String get(String key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void remove(String key) {cache.remove(key);}

  @Override
  public void clear() {cache.clear();}

  @Override
  public int size() {
    throw new UnsupportedOperationException();
  }

  @Override
  protected void dumpState(StateDumpCollector stateDumpCollector) {
    stateDumpCollector.addState("cacheName", cache.getName());
    stateDumpCollector.addState("cacheSize", String.valueOf(cache.size()));
  }

}
