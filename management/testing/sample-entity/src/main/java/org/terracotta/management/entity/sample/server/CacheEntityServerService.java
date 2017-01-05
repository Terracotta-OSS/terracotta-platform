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
import org.terracotta.entity.BasicServiceConfiguration;
import org.terracotta.entity.ServiceRegistry;
import org.terracotta.management.entity.sample.Cache;
import org.terracotta.management.entity.sample.server.management.Management;
import org.terracotta.management.service.monitoring.EntityEventListenerAdapter;
import org.terracotta.management.service.monitoring.EntityEventService;
import org.terracotta.voltron.proxy.SerializationCodec;
import org.terracotta.voltron.proxy.server.ProxyServerEntityService;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @author Mathieu Carbou
 */
public class CacheEntityServerService extends ProxyServerEntityService<Cache, String, CacheSync, Void, Void> {

  private static final Logger LOGGER = LoggerFactory.getLogger(CacheEntityServerService.class);

  public CacheEntityServerService() {
    super(Cache.class, String.class, new Class<?>[]{Serializable[].class}, CacheSync.class, null, null);
    setCodec(new SerializationCodec());
  }

  @Override
  public ActiveCacheServerEntity createActiveEntity(ServiceRegistry registry, String identifier) {
    LOGGER.trace("createActiveEntity({})", identifier);
    Map<String, String> data = registry.getService(new MapConfiguration(identifier));
    ServerCache cache = new ServerCache(identifier, data);
    Management management = new Management(cache.getName(), registry, true);

    ActiveCacheServerEntity entity = new ActiveCacheServerEntity(cache, management);

    // workaround for https://github.com/Terracotta-OSS/terracotta-core/issues/426
    EntityEventService entityEventService = Objects.requireNonNull(registry.getService(new BasicServiceConfiguration<>(EntityEventService.class)));
    entityEventService.addEntityEventListener(new EntityEventListenerAdapter() {
      @Override
      public void onCreated() {
        LOGGER.trace("[{}] onCreated()", identifier);
        management.init();
        management.serverCacheCreated(cache);
      }
    });

    return entity;
  }

  @Override
  protected PassiveCacheServerEntity createPassiveEntity(ServiceRegistry registry, String identifier) {
    LOGGER.trace("createPassiveEntity({})", identifier);
    Map<String, String> data = registry.getService(new MapConfiguration(identifier));
    ServerCache cache = new ServerCache(identifier, data);
    Management management = new Management(cache.getName(), registry, false);
    return new PassiveCacheServerEntity(cache, management);
  }

  @Override
  protected Set<Integer> getKeysForSynchronization() {
    return Collections.singleton(Cache.MUTATION_KEY);
  }

  @Override
  public long getVersion() {
    return 1;
  }

  @Override
  public boolean handlesEntityType(String typeName) {
    return "org.terracotta.management.entity.sample.client.CacheEntity".equals(typeName);
  }

}
