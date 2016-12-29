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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.entity.ClientDescriptor;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Mathieu Carbou
 */
public class DefaultEntityEventService implements TopologyEventListener, EntityEventService {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultEntityEventService.class);

  private final long consumerId;
  private final List<EntityEventListener> listeners = new CopyOnWriteArrayList<>();

  DefaultEntityEventService(long consumerId) {
    this.consumerId = consumerId;
  }

  @Override
  public void onEntityCreated(long consumerId) {
    if (consumerId == this.consumerId) {
      LOGGER.trace("[{}] onEntityCreated()", consumerId);
      for (EntityEventListener listener : listeners) {
        listener.onCreated();
      }
    }
  }

  @Override
  public void onFetch(long consumerId, ClientDescriptor clientDescriptor) {
    if (consumerId == this.consumerId) {
      LOGGER.trace("[{}] onFetch({})", consumerId, clientDescriptor);
      for (EntityEventListener listener : listeners) {
        listener.onFetch(clientDescriptor);
      }
    }
  }

  @Override
  public void onUnfetch(long consumerId, ClientDescriptor clientDescriptor) {
    if (consumerId == this.consumerId) {
      LOGGER.trace("[{}] onUnfetch({})", consumerId, clientDescriptor);
      for (EntityEventListener listener : listeners) {
        listener.onUnfetch(clientDescriptor);
      }
    }
  }

  @Override
  public void onEntityDestroyed(long consumerId) {
    if (consumerId == this.consumerId) {
      LOGGER.trace("[{}] onEntityDestroyed()", consumerId);
      for (EntityEventListener listener : listeners) {
        listener.onDestroyed();
      }
      clear();
    }
  }

  @Override
  public long getConsumerId() {
    return consumerId;
  }

  @Override
  public void addEntityEventListener(EntityEventListener entityEventListener) {
    LOGGER.trace("[{}] addEntityEventListener({})", consumerId, entityEventListener.getClass().getName());
    listeners.add(entityEventListener);
  }

  @Override
  public void onBecomeActive() {
    // do not clear any state because on failover, onBecomeActive() is called after the new active entities are created
    // so it would clear the listeners added by entities at creation time
  }

  void clear() {
    LOGGER.trace("[{}] clear()", consumerId);
    listeners.clear();
  }

}
