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


import org.terracotta.entity.ActiveServerEntity;

/**
 * Registry of all managed objects for a given managed entity instance and type.
 *
 * @author RKAV
 */
public interface ManagedEntityRegistry<E extends ActiveServerEntity<?, ?>> {
  /**
   * Create or lookup the managed object registry of this entity for the given managedType.
   *
   * @param managedType The type of managed object
   * @param <O> managed object type
   * @return the registry entry for the given managed object type.
   */
  <O> ManagedObjectRegistry<O> createOrLookupManagedObjectRegistry(Class<O> managedType);

  /**
   * Remove the managed object registry of the specified managed type.
   *
   * @param managedType The type of managed object
   * @param <O> managed object type
   * @return the removed registry object.
   */
  <O> ManagedObjectRegistry<O> removeManagedObjectRegistry(Class<O> managedType);

  /**
   * Get Registry index.
   *
   * Gets the index of the entity within the registry
   */
  int getRegistryIndex();
}
