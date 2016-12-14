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

import org.terracotta.entity.ServiceRegistry;
import org.terracotta.management.entity.sample.Cache;
import org.terracotta.voltron.proxy.SerializationCodec;
import org.terracotta.voltron.proxy.server.ProxyServerEntityService;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

/**
 * @author Mathieu Carbou
 */
public class CacheEntityServerService extends ProxyServerEntityService<Cache, String, CacheSync> {

  public CacheEntityServerService() {
    super(Cache.class, String.class, new Class<?>[]{Serializable[].class}, CacheSync.class);
    setCodec(new SerializationCodec());
  }

  @Override
  public ActiveCacheServerEntity createActiveEntity(ServiceRegistry registry, String identifier) {
    ServerCache cache = registry.getService(new ServerCacheConfiguration(identifier));
    return new ActiveCacheServerEntity(cache, registry);
  }

  @Override
  protected PassiveCacheServerEntity createPassiveEntity(ServiceRegistry registry, String identifier) {
    ServerCache cache = registry.getService(new ServerCacheConfiguration(identifier));
    return new PassiveCacheServerEntity(cache, registry);
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
