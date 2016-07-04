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
package org.terracotta.voltron.management.consumer;

import org.terracotta.management.capabilities.Capability;
import org.terracotta.management.context.ContextContainer;

import java.util.Collection;
import java.util.Map;

/**
 * Interface for consumer (i.e management entity) to look at capabilities of
 * registered managed entities and their registered managed objects.
 *
 * @author RKAV
 */
public interface CapabilityConsumer {
  /**
   * Get the management contexts of all entities required to make use of the
   * registered objects' capabilities.
   *
   * @return a collection of contexts.
   */
  Collection<ContextContainer> getContexts();

  /**
   * Get the management capabilities of all the registered objects across several entities
   *
   * @return a map of capabilities, where the key is the {@code entityName}.
   */
  Map<String, Collection<Capability>> getCapabilities();
}
