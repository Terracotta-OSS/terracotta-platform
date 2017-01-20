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
package org.terracotta.management.entity.sample.server.management;

import org.terracotta.management.model.capabilities.descriptors.Descriptor;
import org.terracotta.management.model.capabilities.descriptors.Settings;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.registry.Named;
import org.terracotta.management.registry.RequiredContext;
import org.terracotta.management.service.monitoring.registry.provider.AliasBindingManagementProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

@Named("ServerCacheSettings")
@RequiredContext({@Named("consumerId"), @Named("type"), @Named("alias")})
class ServerCacheSettingsManagementProvider extends AliasBindingManagementProvider<ServerCacheBinding> {

  ServerCacheSettingsManagementProvider() {
    super(ServerCacheBinding.class);
  }

  @Override
  public Collection<? extends Descriptor> getDescriptors() {
    Collection<Descriptor> descriptors = new ArrayList<>(super.getDescriptors());
    descriptors.add(new Settings()
        .set("type", getCapabilityName())
        .set("time", System.currentTimeMillis()));

    return descriptors;
  }

  @Override
  protected ExposedServerCacheBinding internalWrap(Context context, ServerCacheBinding managedObject) {
    return new ExposedServerCacheBinding(context, managedObject);
  }

  private static class ExposedServerCacheBinding extends ExposedAliasBinding<ServerCacheBinding> {

    ExposedServerCacheBinding(Context context, ServerCacheBinding binding) {
      super(context.with("type", "ServerCache"), binding);
    }

    @Override
    public Collection<? extends Descriptor> getDescriptors() {
      return Collections.singleton(getSettings());
    }

    Settings getSettings() {
      return new Settings(getContext())
          .set("size", getBinding().getValue().size());
    }
  }

}
