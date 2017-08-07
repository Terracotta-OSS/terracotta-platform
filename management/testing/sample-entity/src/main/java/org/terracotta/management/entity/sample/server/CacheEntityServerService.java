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
import org.terracotta.entity.ConfigurationException;
import org.terracotta.entity.ServiceException;
import org.terracotta.entity.ServiceRegistry;
import org.terracotta.management.entity.sample.Cache;
import org.terracotta.management.entity.sample.server.management.Management;
import org.terracotta.voltron.proxy.SerializationCodec;
import org.terracotta.voltron.proxy.server.Messenger;
import org.terracotta.voltron.proxy.server.ProxyServerEntityService;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * @author Mathieu Carbou
 */
public class CacheEntityServerService extends ProxyServerEntityService<String, CacheSync, Void, Messenger> {

  private static final Logger LOGGER = LoggerFactory.getLogger(CacheEntityServerService.class);

  public CacheEntityServerService() {
    super(Cache.class, String.class, new Class<?>[]{Serializable[].class}, CacheSync.class, null, null);
    setCodec(new SerializationCodec());
  }

  @SuppressWarnings("unchecked")
  @Override
  public ActiveCacheServerEntity createActiveEntity(ServiceRegistry registry, String identifier) throws ConfigurationException {
    LOGGER.trace("createActiveEntity({})", identifier);
    try {
      Map<String, String> data = registry.getService(new MapConfiguration(identifier));
      ServerCache cache = new ServerCache(identifier, data);
      Management management = new Management(cache.getName(), registry, true);
      return new ActiveCacheServerEntity(cache, management, registry);
    } catch (ServiceException e) {
      throw new ConfigurationException("Unable to retrieve service: " + e.getMessage());
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  protected PassiveCacheServerEntity createPassiveEntity(ServiceRegistry registry, String identifier) throws ConfigurationException {
    LOGGER.trace("createPassiveEntity({})", identifier);
    try {
      Map<String, String> data = registry.getService(new MapConfiguration(identifier));
      ServerCache cache = new ServerCache(identifier, data);
      Management management = new Management(cache.getName(), registry, false);
      return new PassiveCacheServerEntity(cache, management, registry);
    } catch (ServiceException e) {
      throw new ConfigurationException("Unable to retrieve service: " + e.getMessage());
    }
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
