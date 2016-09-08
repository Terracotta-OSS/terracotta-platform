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
package org.terracotta.management.service.registry;

import org.terracotta.management.model.context.ContextContainer;
import org.terracotta.management.registry.AbstractManagementRegistry;

/**
 * @author Mathieu Carbou
 */
class NoopConsumerManagementRegistry extends AbstractManagementRegistry implements ConsumerManagementRegistry {

  private final long consumerId;

  NoopConsumerManagementRegistry(long consumerId) {
    this.consumerId = consumerId;
  }

  @Override
  public synchronized void refresh() {
  }

  @Override
  public void close() {
  }

  @Override
  public ContextContainer getContextContainer() {
    return new ContextContainer("entityConsumerId", String.valueOf(consumerId));
  }

}
