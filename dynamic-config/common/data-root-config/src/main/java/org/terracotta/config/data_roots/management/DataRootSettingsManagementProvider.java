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
package org.terracotta.config.data_roots.management;

import org.terracotta.management.model.capabilities.descriptors.Settings;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.registry.Named;
import org.terracotta.management.registry.RequiredContext;
import org.terracotta.management.service.monitoring.registry.provider.AliasBindingManagementProvider;

import java.util.Collection;
import java.util.Collections;

@Named("DataRootSettings")
@RequiredContext({@Named("consumerId"), @Named("type"), @Named("alias")})
public class DataRootSettingsManagementProvider extends AliasBindingManagementProvider<DataRootBinding> {

  public DataRootSettingsManagementProvider() {
    super(DataRootBinding.class);
  }

  @Override
  protected ExposedDataRootBinding internalWrap(Context context, DataRootBinding managedObject) {
    return new ExposedDataRootBinding(context, managedObject);
  }

  private static class ExposedDataRootBinding extends ExposedAliasBinding<DataRootBinding> {

    ExposedDataRootBinding(Context context, DataRootBinding binding) {
      super(context.with("type", "DataRootConfig"), binding);
    }

    @Override
    public Collection<? extends Settings> getDescriptors() {
      return Collections.singleton(new Settings(getContext())
        .set("path", getBinding().getValue().toAbsolutePath().toString())
      );
    }
  }

}
