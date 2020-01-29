/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.config.data_roots.management;

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
