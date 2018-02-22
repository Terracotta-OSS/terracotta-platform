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
import org.terracotta.management.model.call.ContextualReturn;
import org.terracotta.management.model.cluster.ManagementRegistry;
import org.terracotta.management.model.notification.ContextualNotification;
import org.terracotta.management.model.stats.ContextualStatistics;
import org.terracotta.monitoring.IStripeMonitoring;
import org.terracotta.monitoring.PlatformServer;

import java.io.Serializable;
import java.util.Objects;

import static org.terracotta.management.service.monitoring.DefaultEntityMonitoringService.RELIABLE_CHANNEL_KEY;
import static org.terracotta.management.service.monitoring.DefaultEntityMonitoringService.UNRELIABLE_CHANNEL_KEY;

/**
 * Adapts the API-wanted {@link ManagementDataListener} into the current existing one ({@link org.terracotta.monitoring.IStripeMonitoring}),
 * that is still currently using addNode / removeNode methods linked to a tree structure and onManagementMessage
 * <p>
 * This class's goal is to receive states and data from any consumer, even from passive servers
 *
 * @author Mathieu Carbou
 */
final class IStripeMonitoringDataListenerAdapter implements IStripeMonitoring {

  private static final Logger LOGGER = LoggerFactory.getLogger(IStripeMonitoringDataListenerAdapter.class);

  private final long consumerId;
  private final ManagementDataListener delegate;

  IStripeMonitoringDataListenerAdapter(long consumerId, ManagementDataListener delegate) {
    this.consumerId = consumerId;
    this.delegate = Objects.requireNonNull(delegate);
  }

  @Override
  public boolean addNode(PlatformServer sender, String[] parents, String name, Serializable value) {
    LOGGER.trace("[{}] addNode({}, {})", consumerId, name, String.valueOf(value));
    if (parents != null && parents.length == 1 && RELIABLE_CHANNEL_KEY.equals(parents[0])) {
      if (value instanceof ManagementMessage) {
        fire((ManagementMessage) value);
      } else if (value != null) {
        Utils.warnOrAssert(LOGGER, "[0] addNode(from={}) IGNORED: wrong value type: {}", name, value.getClass().getSimpleName());
      }
    }
    return false; // false to avoid replay of the cached data when a passive server becomes active
  }

  @Override
  public boolean removeNode(PlatformServer sender, String[] parents, String name) {
    return true;
  }

  @Override
  public void pushBestEffortsData(PlatformServer sender, String name, Serializable data) {
    if (UNRELIABLE_CHANNEL_KEY.equals(name) && data instanceof ManagementMessage) {
      fire((ManagementMessage) data);
    }
  }

  @Override
  public void serverDidBecomeActive(PlatformServer self) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void serverDidJoinStripe(PlatformServer server) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void serverDidLeaveStripe(PlatformServer server) {
    throw new UnsupportedOperationException();
  }

  private void fire(ManagementMessage managementMessage) {
    switch (managementMessage.getType()) {
      case MANAGEMENT_ANSWER: {
        Object[] oo = (Object[]) managementMessage.getData();
        delegate.onCallAnswer(managementMessage.getMessageSource(), (String) oo[0], (ContextualReturn<?>) oo[1]);
        break;
      }
      case REGISTRY: {
        delegate.onRegistry(managementMessage.getMessageSource(), (ManagementRegistry) managementMessage.getData());
        break;
      }
      case STATISTICS: {
        delegate.onStatistics(managementMessage.getMessageSource(), (ContextualStatistics[]) managementMessage.getData());
        break;
      }
      case NOTIFICATION: {
        delegate.onNotification(managementMessage.getMessageSource(), (ContextualNotification) managementMessage.getData());
        break;
      }
      default: {
        throw new AssertionError(managementMessage.getType());
      }
    }
  }

}
