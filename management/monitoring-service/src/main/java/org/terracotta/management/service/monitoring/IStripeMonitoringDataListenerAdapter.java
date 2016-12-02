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
import org.terracotta.monitoring.IStripeMonitoring;
import org.terracotta.monitoring.PlatformServer;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

/**
 * Adapts the API-wanted {@link DataListener} into the current existing one ({@link org.terracotta.monitoring.IStripeMonitoring}),
 * that is still currently using addNode / removeNode methods linked to a tree structure and pushBestEffortsData
 * <p>
 * This class's goal is to receive states and data from any consumer, even from passive servers
 *
 * @author Mathieu Carbou
 */
final class IStripeMonitoringDataListenerAdapter implements IStripeMonitoring {

  private static final Logger LOGGER = LoggerFactory.getLogger(IStripeMonitoringDataListenerAdapter.class);

  private final DataListener delegate;
  private final long consumerId;

  IStripeMonitoringDataListenerAdapter(long consumerId, DataListener delegate) {
    this.delegate = Objects.requireNonNull(delegate);
    this.consumerId = consumerId;
  }

  @Override
  public boolean addNode(PlatformServer sender, String[] parents, String name, Serializable value) {
    LOGGER.trace("[{}] addNode({}, {}, {})", consumerId, sender.getServerName(), Arrays.toString(parents), name);
    if (parents == null) {
      parents = new String[0];
    }
    String[] path = Arrays.copyOf(parents, parents.length + 1);
    path[parents.length] = name;
    delegate.setState(consumerId, sender, path, value);
    return true;
  }

  @Override
  public boolean removeNode(PlatformServer sender, String[] parents, String name) {
    LOGGER.trace("[{}] removeNode({}, {})", consumerId, sender.getServerName(), Arrays.toString(parents));
    if (parents == null) {
      parents = new String[0];
    }
    String[] path = Arrays.copyOf(parents, parents.length + 1);
    path[parents.length] = name;
    delegate.setState(consumerId, sender, path, null);
    return true;
  }

  @Override
  public void pushBestEffortsData(PlatformServer sender, String name, Serializable data) {
    delegate.pushBestEffortsData(consumerId, sender, name, data);
  }

  @Override
  public void serverDidBecomeActive(PlatformServer self) {
    throw new UnsupportedOperationException(String.valueOf(consumerId));
  }

  @Override
  public void serverDidJoinStripe(PlatformServer server) {
    throw new UnsupportedOperationException(String.valueOf(consumerId));
  }

  @Override
  public void serverDidLeaveStripe(PlatformServer server) {
    throw new UnsupportedOperationException(String.valueOf(consumerId));
  }

}
