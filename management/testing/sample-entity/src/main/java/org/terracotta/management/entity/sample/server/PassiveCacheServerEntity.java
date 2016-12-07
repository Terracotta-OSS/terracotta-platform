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
import org.terracotta.voltron.proxy.server.PassiveProxiedServerEntity;

/**
 * @author Mathieu Carbou
 */
class PassiveCacheServerEntity extends PassiveProxiedServerEntity<Cache, CacheSync> {

  private static final Logger LOGGER = LoggerFactory.getLogger(PassiveCacheServerEntity.class);

  private final Management management;
  private final ServerCache cache;

  PassiveCacheServerEntity(ServerCache cache, ServiceRegistry serviceRegistry) {
    super(cache, cache);
    this.cache = cache;
    this.management = new Management(cache.getName(), serviceRegistry, false);
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
    LOGGER.trace("[{}] startSyncConcurrencyKey({})", concurrencyKey);
  }

  @Override
  public void endSyncConcurrencyKey(int concurrencyKey) {
    LOGGER.trace("[{}] endSyncConcurrencyKey({})", concurrencyKey);
  }
}
