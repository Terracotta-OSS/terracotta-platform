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
package org.terracotta.management.registry;

import org.terracotta.management.model.capabilities.Capability;
import org.terracotta.management.model.context.ContextContainer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Ludovic Orban
 * @author Mathieu Carbou
 */
public class DefaultManagementRegistry implements ManagementRegistry {

  private final ContextContainer contextContainer;

  private static final Comparator<Capability> CAPABILITY_COMPARATOR = new Comparator<Capability>() {
    @Override
    public int compare(Capability o1, Capability o2) {
      return o1.getName().compareTo(o2.getName());
    }
  };

  protected final List<ManagementProvider<?>> managementProviders = new CopyOnWriteArrayList<ManagementProvider<?>>();

  public DefaultManagementRegistry(ContextContainer contextContainer) {
    this.contextContainer = contextContainer; // accept null values - can be overridden
  }

  @Override
  public void addManagementProvider(ManagementProvider<?> provider) {
    String name = provider.getCapabilityName();
    for (ManagementProvider<?> managementProvider : managementProviders) {
      if (managementProvider.getCapabilityName().equals(name)) {
        throw new IllegalStateException("Duplicated management provider name : " + name);
      }
    }
    managementProviders.add(provider);
  }

  @Override
  public void removeManagementProvider(ManagementProvider<?> provider) {
    managementProviders.remove(provider);
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean register(Object managedObject) {
    boolean b = false;
    for (ManagementProvider managementProvider : managementProviders) {
      if (managementProvider.getManagedType().isInstance(managedObject)) {
        b |= managementProvider.register(managedObject) != null;
      }
    }
    return b;
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean unregister(Object managedObject) {
    boolean b = false;
    for (ManagementProvider managementProvider : managementProviders) {
      if (managementProvider.getManagedType().isInstance(managedObject)) {
        b |= managementProvider.unregister(managedObject) != null;
      }
    }
    return b;
  }

  @Override
  public CapabilityManagement withCapability(String capabilityName) {
    return new DefaultCapabilityManagement(this, capabilityName);
  }

  @Override
  public Collection<? extends Capability> getCapabilities() {
    List<Capability> capabilities = new ArrayList<Capability>();
    for (ManagementProvider<?> managementProvider : managementProviders) {
      capabilities.add(managementProvider.getCapability());
    }
    Collections.sort(capabilities, CAPABILITY_COMPARATOR);
    return capabilities;
  }

  @Override
  public List<ManagementProvider<?>> getManagementProvidersByCapability(String capabilityName) {
    List<ManagementProvider<?>> allProviders = new ArrayList<ManagementProvider<?>>();
    for (ManagementProvider<?> provider : managementProviders) {
      if (provider.getCapabilityName().equals(capabilityName)) {
        allProviders.add(provider);
      }
    }
    return allProviders;
  }

  @Override
  public ContextContainer getContextContainer() {
    return contextContainer;
  }
}
