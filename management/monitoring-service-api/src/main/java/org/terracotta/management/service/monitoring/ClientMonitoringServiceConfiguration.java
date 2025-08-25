/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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

import org.terracotta.entity.BasicServiceConfiguration;
import org.terracotta.entity.ClientCommunicator;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceException;
import org.terracotta.entity.ServiceRegistry;

import java.util.Objects;

/**
 * @author Mathieu Carbou
 */
public class ClientMonitoringServiceConfiguration implements ServiceConfiguration<ClientMonitoringService> {
  
  private final ServiceRegistry registry;

  public ClientMonitoringServiceConfiguration(ServiceRegistry registry) {
    this.registry = Objects.requireNonNull(registry);
  }

  @Override
  public Class<ClientMonitoringService> getServiceType() {
    return ClientMonitoringService.class;
  }

  public ClientCommunicator getClientCommunicator() {
    try {
      return Objects.requireNonNull(registry.getService(new BasicServiceConfiguration<>(ClientCommunicator.class)));
    } catch (ServiceException e) {
      // IMonitoringProducer is a mandatory platform service
      throw new AssertionError(e);
    }
  }
}
