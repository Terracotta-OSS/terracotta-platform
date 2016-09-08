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

import org.terracotta.management.model.capabilities.Capability;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.context.ContextContainer;
import org.terracotta.management.model.message.DefaultMessage;
import org.terracotta.management.model.notification.ContextualNotification;
import org.terracotta.management.registry.AbstractManagementRegistry;
import org.terracotta.management.sequence.SequenceGenerator;
import org.terracotta.monitoring.IMonitoringProducer;

import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Mathieu Carbou
 */
class DefaultConsumerManagementRegistry extends AbstractManagementRegistry implements ConsumerManagementRegistry {

  private final IMonitoringProducer producer;
  private final SequenceGenerator sequenceGenerator;
  private final AtomicBoolean dirty = new AtomicBoolean();
  private final ContextContainer contextContainer;

  DefaultConsumerManagementRegistry(long consumerId, IMonitoringProducer producer, SequenceGenerator sequenceGenerator) {
    this.producer = Objects.requireNonNull(producer);
    this.sequenceGenerator = Objects.requireNonNull(sequenceGenerator);
    this.contextContainer = new ContextContainer("entityConsumerId", String.valueOf(consumerId));
  }

  @Override
  public boolean register(Object managedObject) {
    boolean b = super.register(managedObject);
    if (b) {
      dirty.set(true);
    }
    return b;
  }

  @Override
  public boolean unregister(Object managedObject) {
    boolean b = super.unregister(managedObject);
    if (b) {
      dirty.set(true);
    }
    return b;
  }

  @Override
  public synchronized void refresh() {
    if (dirty.compareAndSet(true, false)) {
      String[] path = {"registry"};
      Collection<Capability> capabilities = getCapabilities();
      Capability[] capabilitiesArray = capabilities.toArray(new Capability[capabilities.size()]);

      producer.addNode(new String[0], path[0], null);
      producer.addNode(path, "contextContainer", contextContainer);
      producer.addNode(path, "capabilities", capabilitiesArray);

      producer.pushBestEffortsData("entity-notifications", new DefaultMessage(
          sequenceGenerator.next(),
          "NOTIFICATION",
          new ContextualNotification(Context.create(contextContainer.getName(), contextContainer.getValue()), "ENTITY_REGISTRY_UPDATED")));
    }
  }

  @Override
  public void close() {
    producer.removeNode(new String[0], "registry");
  }

  @Override
  public ContextContainer getContextContainer() {
    return contextContainer;
  }

}
