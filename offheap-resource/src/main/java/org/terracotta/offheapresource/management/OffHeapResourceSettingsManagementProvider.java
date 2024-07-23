/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
package org.terracotta.offheapresource.management;

import org.terracotta.management.model.capabilities.descriptors.Descriptor;
import org.terracotta.management.model.capabilities.descriptors.Settings;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.registry.Named;
import org.terracotta.management.registry.RequiredContext;
import org.terracotta.management.service.monitoring.registry.provider.AliasBindingManagementProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

@Named("OffHeapResourceSettings")
@RequiredContext({@Named("consumerId"), @Named("type"), @Named("alias")})
public class OffHeapResourceSettingsManagementProvider extends AliasBindingManagementProvider<OffHeapResourceBinding> {

  public OffHeapResourceSettingsManagementProvider() {
    super(OffHeapResourceBinding.class);
  }

  @Override
  public Collection<Descriptor> getDescriptors() {
    Collection<Descriptor> descriptors = new ArrayList<>(super.getDescriptors());
    descriptors.add(new Settings()
      .set("type", "OffHeapResourceSettingsManagementProvider")
      .set("time", System.currentTimeMillis()));
    return descriptors;
  }

  @Override
  protected ExposedOffHeapResourceBinding internalWrap(Context context, OffHeapResourceBinding managedObject) {
    return new ExposedOffHeapResourceBinding(context, managedObject);
  }

  private static class ExposedOffHeapResourceBinding extends ExposedAliasBinding<OffHeapResourceBinding> {

    ExposedOffHeapResourceBinding(Context context, OffHeapResourceBinding binding) {
      super(context.with("type", "OffHeapResource"), binding);
    }

    @Override
    public Collection<? extends Settings> getDescriptors() {
      return Collections.singleton(new Settings(getContext())
        .set("capacity", getBinding().getValue().capacity())
        .set("availableAtTime", getBinding().getValue().available())
      );
    }
  }

}
