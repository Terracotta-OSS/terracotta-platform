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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Ludovic Orban
 * @author Mathieu Carbou
 */
public abstract class AbstractManagementRegistry implements ManagementRegistry {

  protected final List<ManagementProvider<?>> managementProviders = new CopyOnWriteArrayList<ManagementProvider<?>>();

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
        managementProvider.register(managedObject);
        b = true;
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
        managementProvider.unregister(managedObject);
        b = true;
      }
    }
    return b;
  }

  @Override
  public CapabilityManagement withCapability(String capabilityName) {
    return new DefaultCapabilityManagement(this, capabilityName);
  }

  @Override
  public Collection<Capability> getCapabilities() {
    Collection<Capability> capabilities = new ArrayList<Capability>();
    for (ManagementProvider<?> managementProvider : managementProviders) {
      capabilities.add(managementProvider.getCapability());
    }
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

}
