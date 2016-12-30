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
import org.terracotta.entity.ClientDescriptor;

/**
 * @author Mathieu Carbou
 */
@CommonComponent
public interface EntityEventListener {

  /**
   * Callback called when platform told the monitoring service that the entity has been created
   */
  void onCreated();

  /**
   * Callback called when platform told the monitoring service that a fetch happened
   */
  void onFetch(ClientDescriptor clientDescriptor);

  /**
   * Callback called when platform told the monitoring service that an unfetch
   */
  void onUnfetch(ClientDescriptor clientDescriptor);

  /**
   * Callback called when platform told the monitoring service that an entity has been destroyed
   */
  void onDestroyed();

}
