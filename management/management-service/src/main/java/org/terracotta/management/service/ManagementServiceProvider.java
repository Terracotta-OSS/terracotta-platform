/**
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
package org.terracotta.management.service;

import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderConfiguration;
import org.terracotta.management.service.impl.DefaultManagementService;
import org.terracotta.voltron.management.ManagementService;

import java.util.Collection;
import java.util.Collections;

/**
 * Setup management service on the active.
 *
 * @author RKAV
 */
public class ManagementServiceProvider implements ServiceProvider {
  private final ManagementService mgmtService = new DefaultManagementService();

  @Override
  public boolean initialize(ServiceProviderConfiguration serviceProviderConfiguration) {
    return true;
  }

  @Override
  public <T> T getService(long entityID, ServiceConfiguration<T> serviceConfiguration) {
    return serviceConfiguration.getServiceType().cast(mgmtService);
  }

  @Override
  public Collection<Class<?>> getProvidedServiceTypes() {
    return Collections.singleton(ManagementService.class);
  }

  @Override
  public void clear() {
  }
}
