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

/**
 * @author Mathieu Carbou
 */
public interface ManageableServerComponent {

  /**
   * First called by the monitoring service when the NMS entity is asking for a server-level management registry.
   * <p>
   * Here, components (services and plugins) can add their management providers, eventually keep a reference
   * to all incoming registries, and also expose objects.
   * <p>
   * Note: this call could be made several times if several entities are asking for a server-level registry,
   * and entity registries have a life cycle shorter than server plugins and server services.
   *
   * @param registry The server-level registry created for a specific consumer (entity)
   */
  void onManagementRegistryCreated(EntityManagementRegistry registry);

  void onManagementRegistryClose(EntityManagementRegistry registry);
}
