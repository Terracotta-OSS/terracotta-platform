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
import org.terracotta.entity.ServiceRegistry;
import org.terracotta.management.entity.sample.Cache;
import org.terracotta.management.entity.sample.server.management.Management;
import org.terracotta.voltron.proxy.server.ActiveProxiedServerEntity;

import java.io.Serializable;

/**
 * @author Mathieu Carbou
 */
class ActiveCacheServerEntity extends ActiveProxiedServerEntity<Cache, CacheSync, Void> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ActiveCacheServerEntity.class);

  private final Management management;
  private final ServerCache cache;
  private final ServerCache.Listener listener = (key, value) -> fireMessage(Serializable[].class, new Serializable[]{"remove", key, value}, true);

  ActiveCacheServerEntity(ServerCache cache, ServiceRegistry serviceRegistry) {
    super(cache);
    this.cache = cache;

    // callback clients on eviction
    cache.addListener(listener);

    this.management = new Management(cache.getName(), serviceRegistry, true);
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
    cache.removeListener(listener);
    super.destroy();
  }

  @Override
  public void synchronizeKeyToPassive(int concurrencyKey) {
    if (concurrencyKey == Cache.MUTATION_KEY) {
      LOGGER.trace("[{}] synchronize({})", cache.getName(), concurrencyKey);
      getSynchronizer().syncCacheDataInPassives(cache.getData());
    }
  }

}
