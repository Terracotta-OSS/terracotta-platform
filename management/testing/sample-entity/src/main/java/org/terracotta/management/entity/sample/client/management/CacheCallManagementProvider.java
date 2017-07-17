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
package org.terracotta.management.entity.sample.client.management;

import org.terracotta.management.entity.sample.client.ClientCache;
import org.terracotta.management.model.capabilities.descriptors.Descriptor;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.registry.action.AbstractActionManagementProvider;
import org.terracotta.management.registry.action.Exposed;
import org.terracotta.management.registry.ExposedObject;
import org.terracotta.management.registry.Named;
import org.terracotta.management.registry.RequiredContext;

import java.util.Collection;
import java.util.Collections;

/**
 * @author Mathieu Carbou
 */
@Named("CacheCalls")
@RequiredContext({@Named("appName"), @Named("cacheName")})
public class CacheCallManagementProvider extends AbstractActionManagementProvider<ClientCache> {
  private final Context parentContext;

  CacheCallManagementProvider(Context parentContext) {
    super(ClientCache.class);
    this.parentContext = parentContext;
  }

  @Override
  protected ExposedObject<ClientCache> wrap(ClientCache managedObject) {
    return new ExposedClientCache(managedObject, parentContext.with("cacheName", managedObject.getName()));
  }

  public static class ExposedClientCache implements ExposedObject<ClientCache> {

    private final ClientCache clientCache;
    private final Context context;

    ExposedClientCache(ClientCache clientCache, Context context) {
      this.clientCache = clientCache;
      this.context = context;
    }

    @Exposed
    public void clear() {
      clientCache.clear();
    }

    @Exposed
    public void put(@Named("key") String key, @Named("value") String value) {
      clientCache.put(key, value);
    }

    @Exposed
    public String get(@Named("key") String key) {
      return clientCache.get(key);
    }

    @Exposed
    public int size() {
      return clientCache.size();
    }

    @Override
    public ClientCache getTarget() {
      return clientCache;
    }

    @Override
    public ClassLoader getClassLoader() {
      return clientCache.getClass().getClassLoader();
    }

    @Override
    public Context getContext() {
      return context;
    }

    @Override
    public Collection<? extends Descriptor> getDescriptors() {
      return Collections.emptyList();
    }
  }
}
