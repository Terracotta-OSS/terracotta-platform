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
package org.terracotta.voltron.management.consumer;

import org.terracotta.entity.ActiveServerEntity;
import org.terracotta.voltron.management.common.providers.ManagementProvider;

import java.util.Collection;

/**
 * The consumer interface of management service for obtaining management providers.
 * The consumer of a management service is assumed to be a management system.
 * <p>
 * Consumers can use this interface to access the management capabilities of multiple
 * entities on a given active stripe. These capabilities include(s) actions on managed
 * objects and getting statistics from managed object(s) of all entities on the stripe
 * (and possibly clients that are connected to the stripe).
 *
 * @author RKAV
 */
public interface ProviderConsumer {
  /**
   * List all management providers installed for a specific capability across all entities
   *
   * @param capabilityName The capability name
   * @return The list of management providers installed
   */
  Collection<ManagementProvider<?>> getManagementProvidersByCapability(String capabilityName);

  /**
   * Get all management providers installed for a specific entity.
   *
   * @param entityType Type of entity.
   * @param entityName Name/alias of the entity.
   * @param <E> the entity type.
   * @return A collection of management providers configured for the entity given by
   *         {@code entityType} and {@code entityName}.
   */
  <E extends ActiveServerEntity<?, ?>> Collection<ManagementProvider<?>> getManagementProvidersByEntity(Class<E> entityType,
                                                                                      String entityName);

  /**
   * Get all management providers installed for a specific entity and a specific capability.
   *
   * @param entityType Type of entity.
   * @param entityName Name/alias of the entity
   * @param capabilityName The name of the capability.
   * @param <E> Entity type
   * @return A collection of management providers configured for the entity and capability.
   */
  <E extends ActiveServerEntity<?, ?>> Collection<ManagementProvider<?>> getManagementProvidersByEntityAndCapability(Class<E> entityType,
                                                                                                   String entityName,
                                                                                                   String capabilityName);

  /**
   * Get all management providers installed for a specific entity and a specific capability and a
   * specific managed object type.
   *
   * @param entityType Type of managed entity
   * @param entityName Name of managed entity
   * @param objectType Type of managed object
   * @param capabilityName Capability Name
   * @param <E> Entity Type
   * @param <O> Object Type
   * @return A single management provider for the given {@code objectType} and {@code capabilityName}.
   */
  <E extends ActiveServerEntity<?, ?>, O> ManagementProvider<O> getManagementProvidersByEntityAndCapabilityAndManagedObjectType(Class<E> entityType,
                                                                                                              String entityName,
                                                                                                              Class<O> objectType,
                                                                                                              String capabilityName);
}
