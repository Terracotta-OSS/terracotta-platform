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
package org.terracotta.voltron.management.producer.registry;

import org.terracotta.management.capabilities.Capability;
import org.terracotta.management.context.ContextContainer;
import org.terracotta.voltron.management.common.providers.ManagementProvider;

import java.util.Collection;

/**
 * Registry of managed objects for a given object type within a given managed entity.
 *
 * @param <O> The managed object type.
 *
 * @author RKAV
 */
public interface ManagedObjectRegistry<O> {
  /**
   * The class of managed objects.
   *
   * @return the managed object type.
   */
  Class<O> managedType();

  /**
   * Adds to this registry a specific management provider for objects of type O
   *
   * @param provider The management provider instance
   */
  void addManagementProvider(ManagementProvider<O> provider);

  /**
   * Removes from this registry a specific management provider for object types T
   *
   * @param provider The management provider instance
   */
  void removeManagementProvider(ManagementProvider<O> provider);

  /**
   * Register a managed object instance in the management registry.
   *
   * @param managedObject  the managed object.
   */
  void register(O managedObject);

  /**
   * Unregister an object from the management registry.
   *
   * @param managedObject  the managed object.
   */
  void unregister(O managedObject);

  /**
   * Get the management capabilities of the registered objects.
   *
   * @return a collection of capabilities.
   */
  Collection<Capability> getCapabilities();

  /**
   * Get the management context required to make use of the
   * registered objects' capabilities.
   *
   * @return a this management registry context.
   */
  ContextContainer getContext();
}
