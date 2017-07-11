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
package org.terracotta.management.service.monitoring.registry.provider;

import com.tc.classloader.CommonComponent;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.registry.AbstractManagementProvider;
import org.terracotta.management.registry.ManagementProvider;
import org.terracotta.management.registry.ExposedObject;
import org.terracotta.management.service.monitoring.EntityMonitoringService;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * @author Mathieu Carbou
 */
@CommonComponent
public abstract class AbstractEntityManagementProvider<T> extends AbstractManagementProvider<T> implements ManagementProvider<T>, MonitoringServiceAware {

  private EntityMonitoringService monitoringService;

  public AbstractEntityManagementProvider(Class<? extends T> managedType) {
    super(managedType);
  }

  @Override
  public void setMonitoringService(EntityMonitoringService monitoringService) {
    this.monitoringService = Objects.requireNonNull(monitoringService);
  }

  protected EntityMonitoringService getMonitoringService() {
    return Objects.requireNonNull(monitoringService);
  }

  @Override
  public void register(T managedObject) {
    try {
      registerAsync(managedObject).toCompletableFuture().get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    } catch (ExecutionException e) {
      throw new RuntimeException(e.getCause());
    }
  }

  public CompletableFuture<Void> registerAsync(T managedObject) {
    super.register(managedObject);
    return CompletableFuture.completedFuture(null);
  }

  @Override
  protected ExposedObject<T> wrap(T managedObject) {
    Context context = Context.create("consumerId", String.valueOf(getMonitoringService().getConsumerId()));
    return internalWrap(context, managedObject);
  }

  protected abstract ExposedObject<T> internalWrap(Context context, T managedObject);
}
