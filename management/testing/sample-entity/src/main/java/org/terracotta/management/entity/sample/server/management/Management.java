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
package org.terracotta.management.entity.sample.server.management;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.entity.BasicServiceConfiguration;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.ConfigurationException;
import org.terracotta.entity.ServiceException;
import org.terracotta.entity.ServiceRegistry;
import org.terracotta.management.entity.sample.server.ServerCache;
import org.terracotta.management.service.monitoring.ActiveEntityMonitoringServiceConfiguration;
import org.terracotta.management.service.monitoring.ConsumerManagementRegistry;
import org.terracotta.management.service.monitoring.ConsumerManagementRegistryConfiguration;
import org.terracotta.management.service.monitoring.EntityMonitoringService;
import org.terracotta.management.service.monitoring.PassiveEntityMonitoringServiceConfiguration;
import org.terracotta.monitoring.IMonitoringProducer;

import java.util.Objects;

/**
 * @author Mathieu Carbou
 */
public class Management {

  private static final Logger LOGGER = LoggerFactory.getLogger(Management.class);

  private final ConsumerManagementRegistry managementRegistry;
  private final String cacheName;

  public Management(String cacheName, ServiceRegistry serviceRegistry, boolean active) throws ConfigurationException {
    this.cacheName = cacheName;
    EntityMonitoringService monitoringService;

    try {
      if (active) {
        monitoringService = Objects.requireNonNull(serviceRegistry.getService(new ActiveEntityMonitoringServiceConfiguration()));
      } else {
        IMonitoringProducer monitoringProducer = serviceRegistry.getService(new BasicServiceConfiguration<>(IMonitoringProducer.class));
        monitoringService = Objects.requireNonNull(serviceRegistry.getService(new PassiveEntityMonitoringServiceConfiguration(monitoringProducer)));
      }
    } catch (ServiceException e) {
      throw new ConfigurationException("Unable to retrieve service: " + e.getMessage());
    }

    try {
      this.managementRegistry = Objects.requireNonNull(serviceRegistry.getService(new ConsumerManagementRegistryConfiguration(monitoringService)));
    } catch (ServiceException e) {
      throw new ConfigurationException("Unable to retrieve service: " + e.getMessage());
    }
    this.managementRegistry.addManagementProvider(new ServerCacheSettingsManagementProvider());
    this.managementRegistry.addManagementProvider(new ServerCacheCallManagementProvider());
    this.managementRegistry.addManagementProvider(new ServerCacheStatisticsManagementProvider());
    if (active) {
      this.managementRegistry.addManagementProvider(new ClientStateSettingsManagementProvider());
    }
  }

  public void init() {
    if (managementRegistry != null) {
      LOGGER.trace("[{}] init()", cacheName);
      managementRegistry.refresh(); // send to voltron the registry at entity init
    }
  }

  public void serverCacheCreated(ServerCache cache) {
    if (managementRegistry != null) {
      LOGGER.trace("[{}] serverCacheCreated()", cacheName);

      managementRegistry.registerAndRefresh(new ServerCacheBinding(cache))
          .thenRun(() -> managementRegistry.pushServerEntityNotification(new ServerCacheBinding(cache), "SERVER_CACHE_CREATED"));
    }
  }

  public void serverCacheDestroyed(ServerCache cache) {
    if (managementRegistry != null) {
      LOGGER.trace("[{}] serverCacheDestroyed()", cacheName);

      managementRegistry.pushServerEntityNotification(new ServerCacheBinding(cache), "SERVER_CACHE_DESTROYED");
    }
  }

  public void startSync(ServerCache cache) {
    if (managementRegistry != null) {
      LOGGER.trace("[{}] startSync()", cacheName);

      managementRegistry.pushServerEntityNotification(new ServerCacheBinding(cache), "SYNC_START");
    }
  }

  public void endSync(ServerCache cache) {
    if (managementRegistry != null) {
      LOGGER.trace("[{}] endSync()", cacheName);

      managementRegistry.pushServerEntityNotification(new ServerCacheBinding(cache), "SYNC_END");
    }
  }

  public void attach(ClientDescriptor clientDescriptor) {
    if (managementRegistry != null) {
      LOGGER.trace("[{}] attach({})", cacheName, clientDescriptor);

      managementRegistry.registerAndRefresh(new ClientStateBinding(clientDescriptor, true))
          .thenRun(() -> managementRegistry.pushServerEntityNotification(new ClientStateBinding(clientDescriptor, true), "CLIENT_ATTACHED"));
    }
  }

  public void detach(ClientDescriptor clientDescriptor) {
    if (managementRegistry != null) {
      LOGGER.trace("[{}] detach({})", cacheName, clientDescriptor);

      managementRegistry.pushServerEntityNotification(new ClientStateBinding(clientDescriptor, true), "CLIENT_DETACHED");

      managementRegistry.unregisterAndRefresh(new ClientStateBinding(clientDescriptor, false));
      managementRegistry.refresh();
    }
  }

}
