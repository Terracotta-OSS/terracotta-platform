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
package org.terracotta.coremon;

import com.tc.classloader.BuiltinService;
import org.terracotta.entity.PlatformConfiguration;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderConfiguration;
import org.terracotta.management.service.monitoring.EntityManagementRegistry;
import org.terracotta.management.service.monitoring.ManageableServerComponent;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

@BuiltinService
public class CoreManagementServiceProvider implements ServiceProvider, ManageableServerComponent {

  @Override
  public boolean initialize(ServiceProviderConfiguration serviceProviderConfiguration, PlatformConfiguration platformConfiguration) {
    return true;
  }

  @Override
  public <T> T getService(long consumerID, ServiceConfiguration<T> serviceConfiguration) {
    Class<T> serviceType = serviceConfiguration.getServiceType();
    if (serviceType.equals(ManageableServerComponent.class)) {
      return serviceType.cast(this);
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection<Class<?>> getProvidedServiceTypes() {
    return Collections.singleton(ManageableServerComponent.class);
  }

  @Override
  public void prepareForSynchronization() {
  }

  @Override
  public void onManagementRegistryCreated(EntityManagementRegistry registry) {
    try {
    registry.addManagementProvider(new StageStatisticsProvider());
    registry.addManagementProvider(new StageSettingsProvider());

      JmxUtil jmxUtil = new JmxUtil();
      Map<String, Object> statistics = jmxUtil.getStatistics();
      for (String key : statistics.keySet()) {
        if (key.endsWith("queueCount")) {
          String[] split = key.split("\\.");
          String stageName = split[0];
          registry.register(new StageBinding(stageName, jmxUtil));
        }
      }

      registry.refresh();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public void onManagementRegistryClose(EntityManagementRegistry registry) {
  }
}
