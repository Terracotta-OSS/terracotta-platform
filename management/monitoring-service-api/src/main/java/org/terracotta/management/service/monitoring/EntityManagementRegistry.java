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
package org.terracotta.management.service.monitoring;

import com.tc.classloader.CommonComponent;
import org.terracotta.entity.CommonServerEntity;
import org.terracotta.management.model.context.ContextContainer;
import org.terracotta.management.registry.CapabilityManagementSupport;
import org.terracotta.management.registry.ManagementProvider;

import java.io.Closeable;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * An entity can use such registry to expose / unexpose any supported object.
 * This registry is bound to the entity using it, but the underlying exposed data might be shared.
 * <p>
 * When an entity finishes using a registry, it must close it.
 *
 * @author Mathieu Carbou
 */
@CommonComponent
public interface EntityManagementRegistry extends CapabilityManagementSupport, Closeable {

  /**
   * @return the monitoring service associated to this entity
   */
  EntityMonitoringService getMonitoringService();

  /**
   * Get the management context required to make use of the
   * registered objects' capabilities.
   *
   * @return a this management registry context.
   */
  ContextContainer getContextContainer();

  /**
   * Adds to this registry a specific management provider for object types T.
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
  CompletableFuture<Void> register(Object managedObject);

  /**
   * Unregister an object from the management registry.
   *
   * @param managedObject the managed object.
   */
  void unregister(Object managedObject);

  /**
   * Used to force an update of the metadata exposed in the server.
   */
  void refresh();

  default void pushServerEntityNotification(Object managedObjectSource, String type) {
    pushServerEntityNotification(managedObjectSource, type, Collections.emptyMap());
  }

  boolean pushServerEntityNotification(Object managedObjectSource, String type, Map<String, String> attrs);

  default CompletableFuture<Void> registerAndRefresh(Object managedObject) {
    return register(managedObject).thenRun(this::refresh);
  }

  default void unregisterAndRefresh(Object managedObject) {
    unregister(managedObject);
    refresh();
  }

  /**
   * Closes this service from {@link CommonServerEntity#destroy()}
   */
  @Override
  void close();
}
