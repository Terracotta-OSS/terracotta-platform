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
package org.terracotta.client.message.tracker;

import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.PlatformConfiguration;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderCleanupException;
import org.terracotta.entity.ServiceProviderConfiguration;
import org.terracotta.entity.StateDumper;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ClientMessageTrackerProvider implements ServiceProvider {

  private ConcurrentMap<String, ClientMessageTracker> serviceMap = new ConcurrentHashMap<>();
  @Override
  public boolean initialize(ServiceProviderConfiguration serviceProviderConfiguration, PlatformConfiguration platformConfiguration) {
    return true;
  }

  @Override
  public <T> T getService(long l, ServiceConfiguration<T> serviceConfiguration) {
    if (serviceConfiguration instanceof ClientMessageTrackerConfiguration) {
      ClientMessageTrackerConfiguration cmtServiceConfiguration = (ClientMessageTrackerConfiguration) serviceConfiguration;
      ClientMessageTracker clientMessageTracker = serviceMap.computeIfAbsent(cmtServiceConfiguration.getEntityIdentifier(),
          id -> new ClientMessageTrackerImpl(cmtServiceConfiguration.getTrackerPolicy()));
      return serviceConfiguration.getServiceType().cast(clientMessageTracker);
    }
    throw new IllegalArgumentException("Unexpected configuration type: " + serviceConfiguration);
  }

  @Override
  public Collection<Class<?>> getProvidedServiceTypes() {
    return Collections.singletonList(ClientMessageTracker.class);
  }

  @Override
  public void prepareForSynchronization() throws ServiceProviderCleanupException {
    //Nothing to do here
  }

  @Override
  public void dumpStateTo(StateDumper stateDumper) {
    for (Map.Entry<String, ClientMessageTracker> entry : serviceMap.entrySet()) {
      entry.getValue().dumpStateTo(stateDumper.subStateDumper(entry.getKey().toString()));
    }
  }
}
