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
import org.terracotta.management.model.capabilities.descriptors.Settings;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.registry.AbstractManagementProvider;
import org.terracotta.management.registry.ExposedObject;
import org.terracotta.management.registry.Named;
import org.terracotta.management.registry.RequiredContext;

import java.util.Collection;
import java.util.Collections;

/**
 * @author Mathieu Carbou
 */
@Named("CacheSettings")
@RequiredContext({@Named("appName"), @Named("cacheName")})
class CacheSettingsManagementProvider extends AbstractManagementProvider<ClientCache> {
  private final Context parentContext;

  CacheSettingsManagementProvider(Context parentContext) {
    super(ClientCache.class);
    this.parentContext = parentContext;
  }

  @Override
  protected ExposedObject<ClientCache> wrap(ClientCache managedObject) {
    return new ExposedClientCache(managedObject, parentContext.with("cacheName", managedObject.getName()));
  }


  private static class ExposedClientCache implements ExposedObject<ClientCache> {

    private final ClientCache clientCache;
    private final Context context;

    ExposedClientCache(ClientCache clientCache, Context context) {
      this.clientCache = clientCache;
      this.context = context;
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
      return Collections.singleton(new Settings(context)
          .set("size", clientCache.size()));
    }
  }
}
