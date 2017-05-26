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

import org.terracotta.management.model.context.ContextContainer;

/**
 * Repository of objects exposing capabilities via the management and monitoring facilities.
 *
 * @author Ludovic Orban
 */
public interface ManagementRegistry extends CapabilityManagementSupport {

  /**
   * Adds to this registry a specific management provider for object types T
   *
   * @param provider The management provider instance
   * @return false if the management provider name already exists
   */
  boolean addManagementProvider(ManagementProvider<?> provider);

  /**
   * Removes from this registry a specific management provider for object types T
   *
   * @param provider The management provider instance
   */
  void removeManagementProvider(ManagementProvider<?> provider);

  /**
   * Register an object in the management registry.
   *
   * @param managedObject the managed object.
   */
  void register(Object managedObject);

  /**
   * Unregister an object from the management registry.
   *
   * @param managedObject the managed object.
   */
  void unregister(Object managedObject);

  /**
   * Get the management context required to make use of the
   * registered objects' capabilities.
   *
   * @return a this management registry context.
   */
  ContextContainer getContextContainer();

}
