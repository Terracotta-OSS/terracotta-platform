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

import org.terracotta.entity.ClientDescriptor;

/**
 * @author Mathieu Carbou
 */
interface TopologyEventListener {

  /**
   * Callback called when platform told the monitoring service that a service became active
   */
  void onBecomeActive();

  /**
   * Callback called when platform told the monitoring service that a fetch happened
   */
  void onFetch(long consumerId, ClientDescriptor clientDescriptor);

  /**
   * Callback called when platform told the monitoring service that an unfetch
   */
  void onUnfetch(long consumerId, ClientDescriptor clientDescriptor);

  /**
   * Callback called when platform told the monitoring service that an entity has been destroyed
   */
  void onEntityDestroyed(long consumerId);

  /**
   * Callback called when platform told the monitoring service that an entity has been created
   */
  void onEntityCreated(long consumerId);

}
