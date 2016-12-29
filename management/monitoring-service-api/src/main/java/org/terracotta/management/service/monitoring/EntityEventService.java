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

/**
 * Workaround for https://github.com/Terracotta-OSS/terracotta-core/issues/426.
 * <p>
 * This service allows to register a listener for platform events, to know specifically
 * when platform tells the monitoring service that an entity has been created.
 * <p>
 * This tells you when you can initialize or expose any management states into the monitoring service
 * from a client or entity, especially after a failover because the entity API does not provide the
 * same ordering of events as for entity creation.
 *
 * @author Mathieu Carbou
 */
@CommonComponent
public interface EntityEventService {

  long getConsumerId();

  /**
   * Adds a listener that will listen for monitoring events for this entity consumer.
   * Once the consumer is destroyed, the listeners are automatically removed
   */
  void addEntityEventListener(EntityEventListener entityEventListener);

}
