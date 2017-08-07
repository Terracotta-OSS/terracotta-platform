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
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.ServiceException;
import org.terracotta.entity.ServiceRegistry;
import org.terracotta.entity.StateDumpCollector;
import org.terracotta.management.entity.sample.Cache;
import org.terracotta.management.entity.sample.server.management.Management;
import org.terracotta.voltron.proxy.server.ActiveProxiedServerEntity;
import org.terracotta.voltron.proxy.server.Messenger;

import java.io.Serializable;

/**
 * @author Mathieu Carbou
 */
class ActiveCacheServerEntity extends ActiveProxiedServerEntity<CacheSync, Void, Messenger> implements Cache {

  private static final Logger LOGGER = LoggerFactory.getLogger(ActiveCacheServerEntity.class);

  private final Management management;
  private final ServerCache cache;
  private final ServiceRegistry registry;
  private final ServerCache.Listener listener = (key, value) -> fireMessage(Serializable[].class, new Serializable[]{"remove", key, value}, true);

  ActiveCacheServerEntity(ServerCache cache, Management management, ServiceRegistry registry) {
    this.cache = cache;
    this.registry = registry;

    // callback clients on eviction
    cache.addListener(listener);

    this.management = management;
  }

  @Override
  public void connected(ClientDescriptor clientDescriptor) {
    super.connected(clientDescriptor);
    LOGGER.trace("[{}] connected({})", cache.getName(), clientDescriptor);
    management.attach(clientDescriptor);
  }

  @Override
  public void disconnected(ClientDescriptor clientDescriptor) {
    LOGGER.trace("[{}] disconnected({})", cache.getName(), clientDescriptor);
    management.detach(clientDescriptor);
    super.disconnected(clientDescriptor);
  }

  @Override
  public void destroy() {
    LOGGER.trace("[{}] destroy()", cache.getName());
    cache.removeListener(listener);
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
  public void synchronizeKeyToPassive(int concurrencyKey) {
    if (concurrencyKey == Cache.MUTATION_KEY) {
      LOGGER.trace("[{}] synchronize({})", cache.getName(), concurrencyKey);
      getSynchronizer().syncCacheDataInPassives(cache.getData());
    }
  }

  @Override
  public void createNew() {
    super.createNew();
    LOGGER.trace("[{}] createNew()", cache.getName());
    management.init();
    management.serverCacheCreated(cache);
  }

  @Override
  public void loadExisting() {
    super.loadExisting();
    LOGGER.trace("[{}] loadExisting()", cache.getName());
    management.init();
    management.serverCacheCreated(cache);
  }

  @Override
  public void put(String key, String value) {cache.put(key, value);}

  @Override
  public String get(String key) {return cache.get(key);}

  @Override
  public void remove(String key) {cache.remove(key);}

  @Override
  public void clear() {cache.clear();}

  @Override
  public int size() {return cache.size();}

  @Override
  protected void dumpState(StateDumpCollector dump) {
    dump.addState("cacheName", cache.getName());
    dump.addState("cacheSize", String.valueOf(cache.size()));
  }

}
