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
import org.terracotta.management.model.context.Context;
import org.terracotta.management.registry.Named;
import org.terracotta.management.registry.RequiredContext;
import org.terracotta.management.registry.action.AbstractActionManagementProvider;
import org.terracotta.management.registry.action.Exposed;
import org.terracotta.management.registry.ExposedObject;
import org.terracotta.management.service.monitoring.EntityMonitoringService;
import org.terracotta.management.service.monitoring.registry.provider.MonitoringServiceAware;

import java.util.Collection;
import java.util.Collections;

/**
 * @author Mathieu Carbou
 */
@Named("ServerCacheCalls")
@RequiredContext({@Named("consumerId"), @Named("cacheName")})
public class ServerCacheCallManagementProvider extends AbstractActionManagementProvider<ServerCacheBinding> implements MonitoringServiceAware {

  private EntityMonitoringService monitoringService;

  ServerCacheCallManagementProvider() {
    super(ServerCacheBinding.class);
  }

  @Override
  public void setMonitoringService(EntityMonitoringService monitoringService) {
    this.monitoringService = monitoringService;
  }

  @Override
  protected ExposedObject<ServerCacheBinding> wrap(ServerCacheBinding managedObject) {
    Context context = Context.empty()
        .with("consumerId", String.valueOf(monitoringService.getConsumerId()))
        .with("cacheName", managedObject.getAlias());
    return new ExposedServerCache(managedObject, context);
  }

  public static class ExposedServerCache implements ExposedObject<ServerCacheBinding> {

    private final ServerCacheBinding serverCacheBinding;
    private final Context context;

    ExposedServerCache(ServerCacheBinding serverCacheBinding, Context context) {
      this.serverCacheBinding = serverCacheBinding;
      this.context = context;
    }

    @Exposed
    public void clear() {
      serverCacheBinding.getValue().clear();
    }

    @Exposed
    public void put(@Named("key") String key, @Named("value") String value) {
      serverCacheBinding.getValue().put(key, value);
    }

    @Exposed
    public String get(@Named("key") String key) {
      return serverCacheBinding.getValue().get(key);
    }

    @Exposed
    public int size() {
      return serverCacheBinding.getValue().size();
    }

    @Override
    public ServerCacheBinding getTarget() {
      return serverCacheBinding;
    }

    @Override
    public ClassLoader getClassLoader() {
      return serverCacheBinding.getValue().getClass().getClassLoader();
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
