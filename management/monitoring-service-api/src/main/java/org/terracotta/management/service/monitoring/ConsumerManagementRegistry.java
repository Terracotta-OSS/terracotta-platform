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
import org.terracotta.management.registry.ManagementRegistry;

import java.io.Closeable;
import java.util.Collections;
import java.util.Map;

/**
 * An entity can use such registry to expose / unexpose any supported object.
 * This registry is bound to the entity using it, but the underlying exposed data might be shared.
 * <p>
 * When an entity finishes using a registry, it must close it.
 *
 * @author Mathieu Carbou
 */
@CommonComponent
public interface ConsumerManagementRegistry extends ManagementRegistry, Closeable {

  /**
   * Used to force an update of the metadata exposed in the server.
   */
  void refresh();

  default void pushServerEntityNotification(Object managedObjectSource, String type) {
    pushServerEntityNotification(managedObjectSource, type, Collections.emptyMap());
  }

  boolean pushServerEntityNotification(Object managedObjectSource, String type, Map<String, String> attrs);

  default void registerAndRefresh(Object managedObject) {
    register(managedObject);
    refresh();
  }

  default void unregisterAndRefresh(Object managedObject) {
    unregister(managedObject);
    refresh();
  }

  @Override
  void close();
}
