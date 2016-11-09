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
import org.terracotta.management.model.capabilities.descriptors.Descriptor;
import org.terracotta.management.model.cluster.ClientIdentifier;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.registry.action.ExposedObject;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

@CommonComponent
public class ClientBindingManagementProvider<T extends ClientBinding> extends AbstractConsumerManagementProvider<T> {

  public ClientBindingManagementProvider(Class<? extends T> type) {
    super(type);
  }

  @Override
  public ExposedClientBinding<T> register(T managedObject) {
    if (getManagedType() == managedObject.getClass()) {
      return (ExposedClientBinding<T>) super.register(managedObject);
    }
    return null;
  }

  @Override
  public ExposedClientBinding<T> unregister(T managedObject) {
    if (getManagedType() == managedObject.getClass()) {
      return (ExposedClientBinding<T>) super.unregister(managedObject);
    }
    return null;
  }

  @Override
  protected ExposedClientBinding<T> wrap(T managedObject) {
    ClientIdentifier clientIdentifier = getMonitoringService().getClientIdentifier(managedObject.getClientDescriptor());
    long consumerId = getMonitoringService().getConsumerId();
    return internalWrap(managedObject, consumerId, clientIdentifier);

  }

  protected ExposedClientBinding<T> internalWrap(T managedObject, long consumerId, ClientIdentifier clientIdentifier) {
    return new ExposedClientBinding<>(managedObject, consumerId, clientIdentifier);
  }

  public static class ExposedClientBinding<T extends ClientBinding> implements ExposedObject<T> {

    private final T clientBinding;
    private final Context context;

    public ExposedClientBinding(T clientBinding, long consumerId, ClientIdentifier clientIdentifier) {
      this.clientBinding = Objects.requireNonNull(clientBinding);
      this.context = Context.empty()
          .with("consumerId", String.valueOf(consumerId))
          .with("clientId", clientIdentifier.getClientId());
    }

    public T getClientBinding() {
      return clientBinding;
    }

    @Override
    public Context getContext() {
      return context;
    }

    @Override
    public ClassLoader getClassLoader() {
      return clientBinding.getValue().getClass().getClassLoader();
    }

    @Override
    public T getTarget() {
      return clientBinding;
    }

    @Override
    public Collection<? extends Descriptor> getDescriptors() {
      return Collections.emptyList();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      ExposedClientBinding<?> that = (ExposedClientBinding<?>) o;
      return clientBinding.equals(that.clientBinding);
    }

    @Override
    public int hashCode() {
      return clientBinding.hashCode();
    }
  }
}
