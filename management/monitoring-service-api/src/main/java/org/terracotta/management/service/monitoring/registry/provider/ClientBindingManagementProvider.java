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
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.management.model.capabilities.descriptors.Descriptor;
import org.terracotta.management.model.cluster.ClientIdentifier;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.registry.ExposedObject;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@CommonComponent
public class ClientBindingManagementProvider<T extends ClientBinding> extends AbstractEntityManagementProvider<T> {

  public ClientBindingManagementProvider(Class<? extends T> type) {
    super(type);
  }

  @Override
  public CompletableFuture<Void> registerAsync(T managedObject) {
    // a register call with a data bounded to a client descriptor (client information) should be delayed
    // until the monitoring service is made aware of the fetch information, so that it can translate
    // this client descriptor into an M&M client identifier that uniquely identifies a client and can
    // go over the network
    ClientDescriptor clientDescriptor = managedObject.getClientDescriptor();
    CompletableFuture<ClientIdentifier> stage = getMonitoringService().getClientIdentifier(clientDescriptor);
    return stage.thenCompose(clientIdentifier -> {
      managedObject.resolvedClientIdentifier = clientIdentifier;
      return super.registerAsync(managedObject);
    });
  }

  @Override
  protected ExposedClientBinding<T> wrap(T managedObject) {
    Context context = Context.create("consumerId", String.valueOf(getMonitoringService().getConsumerId()))
        .with("alias", managedObject.resolvedClientIdentifier.getClientId());
    return internalWrap(context, managedObject);
  }

  @Override
  protected ExposedClientBinding<T> internalWrap(Context context, T managedObject) {
    return new ExposedClientBinding<>(context, managedObject);
  }

  @CommonComponent
  public static class ExposedClientBinding<T extends ClientBinding> implements ExposedObject<T> {

    private final T clientBinding;
    private final Context context;

    public ExposedClientBinding(Context context, T clientBinding) {
      this.clientBinding = Objects.requireNonNull(clientBinding);
      this.context = Objects.requireNonNull(context);
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
