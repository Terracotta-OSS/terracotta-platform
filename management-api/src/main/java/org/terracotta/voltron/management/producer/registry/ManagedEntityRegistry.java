/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Entity Management API.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
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
}