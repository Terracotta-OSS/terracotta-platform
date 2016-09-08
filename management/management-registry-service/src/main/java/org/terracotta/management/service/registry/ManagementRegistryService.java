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

import org.terracotta.management.registry.ManagementProvider;
import org.terracotta.management.sequence.SequenceGenerator;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Mathieu Carbou
 */
class ManagementRegistryService {

  private final ConcurrentMap<Long, ConsumerManagementRegistry> registries = new ConcurrentHashMap<>();
  private final SequenceGenerator sequenceGenerator;

  ManagementRegistryService(SequenceGenerator sequenceGenerator) {
    this.sequenceGenerator = sequenceGenerator;
  }

  ConsumerManagementRegistry getManagementRegistry(long consumerID, ConsumerManagementRegistryConfiguration configuration) {
    Collection<ManagementProvider<?>> providers = configuration.getProviders();
    return registries.computeIfAbsent(consumerID, id -> {
      DefaultConsumerManagementRegistry registry = new DefaultConsumerManagementRegistry(consumerID, configuration.getMonitoringProducer(), sequenceGenerator) {
        @Override
        public void close() {
          super.close();
          registries.remove(consumerID);
          for (ManagementProvider<?> provider : providers) {
            provider.close();
          }
        }
      };
      providers.forEach(registry::addManagementProvider);
      return registry;
    });
  }

  void clear() {
    registries.clear();
  }

  ConsumerManagementRegistry getNoopManagementRegistry(long consumerID) {
    return registries.computeIfAbsent(consumerID, id -> new NoopConsumerManagementRegistry(consumerID) {
      @Override
      public void close() {
        super.close();
        registries.remove(consumerID);
      }
    });
  }

}
