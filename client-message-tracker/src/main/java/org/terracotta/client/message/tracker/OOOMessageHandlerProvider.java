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

import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.PlatformConfiguration;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderCleanupException;
import org.terracotta.entity.ServiceProviderConfiguration;
import org.terracotta.entity.StateDumpCollector;

import com.tc.classloader.BuiltinService;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@BuiltinService
public class OOOMessageHandlerProvider implements ServiceProvider {

  private ConcurrentMap<String, OOOMessageHandler<EntityMessage, EntityResponse>> serviceMap = new ConcurrentHashMap<>();

  @Override
  public boolean initialize(ServiceProviderConfiguration serviceProviderConfiguration, PlatformConfiguration platformConfiguration) {
    return true;
  }

  @Override
  public <T> T getService(long l, ServiceConfiguration<T> serviceConfiguration) {
    if (serviceConfiguration instanceof OOOMessageHandlerConfiguration) {
      @SuppressWarnings("unchecked")
      OOOMessageHandlerConfiguration<EntityMessage, EntityResponse> cmtServiceConfiguration =
          (OOOMessageHandlerConfiguration<EntityMessage, EntityResponse>) serviceConfiguration;
      OOOMessageHandler<EntityMessage, EntityResponse> messageHandler = serviceMap.computeIfAbsent(cmtServiceConfiguration.getEntityIdentifier(),
          id -> new OOOMessageHandlerImpl<>(cmtServiceConfiguration.getTrackerPolicy(), cmtServiceConfiguration.getSegments(),
              cmtServiceConfiguration.getSegmentationStrategy()));
      return serviceConfiguration.getServiceType().cast(messageHandler);
    }
    throw new IllegalArgumentException("Unexpected configuration type: " + serviceConfiguration);
  }

  @Override
  public Collection<Class<?>> getProvidedServiceTypes() {
    return Collections.singletonList(OOOMessageHandler.class);
  }

  @Override
  public void prepareForSynchronization() throws ServiceProviderCleanupException {
    //Nothing to do here
  }

  @Override
  public void addStateTo(StateDumpCollector stateDumper) {
    for (Map.Entry<String, OOOMessageHandler<EntityMessage, EntityResponse>> entry : serviceMap.entrySet()) {
      entry.getValue().addStateTo(stateDumper.subStateDumpCollector(entry.getKey().toString()));
    }
  }
}
