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

import com.tc.classloader.BuiltinService;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderCleanupException;
import org.terracotta.entity.ServiceProviderConfiguration;

import java.util.Collection;
import java.util.Collections;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Mathieu Carbou
 */
@BuiltinService
public class ConsumerManagementRegistryProvider implements ServiceProvider {

  private static final Logger LOGGER = Logger.getLogger(ConsumerManagementRegistryProvider.class.getName());
  private static final boolean NOOP;
  private static final String[] DEPS = new String[]{
      "org.terracotta.management.service.monitoring.MonitoringService", // <artifactId>monitoring-service</artifactId>
      "org.terracotta.management.model.cluster.Cluster", // <artifactId>cluster-topology</artifactId>
      "org.terracotta.management.model.cluster.ClientIdentifier", // <artifactId>management-model</artifactId>
      "org.terracotta.management.sequence.SequenceGenerator" // <artifactId>sequence-generator</artifactId>
  };

  // detects if the monitoring service and management stack is there. If not, a NOOP implementation is returned
  static {
    boolean noop = false;
    Collection<String> missingDependencies = new TreeSet<>();
    for (String dep : DEPS) {
      try {
        ConsumerManagementRegistryProvider.class.getClassLoader().loadClass(dep);
      } catch (ClassNotFoundException ignored) {
        noop = true;
        missingDependencies.add(dep);
      }
    }
    if (noop && LOGGER.isLoggable(Level.WARNING)) {
      LOGGER.warning("A no-op " + ConsumerManagementRegistry.class.getSimpleName()
          + " will be used due to missing dependencies from the classpath: "
          + String.join(", ", missingDependencies));
    }
    NOOP = noop;
  }

  @Override
  public void clear() throws ServiceProviderCleanupException {
  }

  @Override
  public boolean initialize(ServiceProviderConfiguration configuration) {
    // @BuiltinService cannot be initialized
    return true;
  }

  @Override
  public <T> T getService(long consumerID, ServiceConfiguration<T> configuration) {
    Class<T> serviceType = configuration.getServiceType();

    if (ConsumerManagementRegistry.class == serviceType) {
      if (configuration instanceof ConsumerManagementRegistryConfiguration) {
        return serviceType.cast(NOOP ?
            newNoopManagementRegistry((ConsumerManagementRegistryConfiguration) configuration) :
            newManagementRegistry((ConsumerManagementRegistryConfiguration) configuration));

      } else {
        throw new IllegalArgumentException("Missing configuration: " + ConsumerManagementRegistryConfiguration.class.getSimpleName());
      }

    } else {
      throw new IllegalArgumentException("Unknown service type " + serviceType.getName());
    }
  }

  @Override
  public Collection<Class<?>> getProvidedServiceTypes() {
    return Collections.singletonList(ConsumerManagementRegistry.class);
  }

  private ConsumerManagementRegistry newManagementRegistry(ConsumerManagementRegistryConfiguration configuration) {
    return new DefaultConsumerManagementRegistry(configuration);
  }

  private ConsumerManagementRegistry newNoopManagementRegistry(ConsumerManagementRegistryConfiguration configuration) {
    return new NoopConsumerManagementRegistry(configuration);
  }

}
