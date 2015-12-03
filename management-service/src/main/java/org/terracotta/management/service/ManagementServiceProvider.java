/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Entity Management Service.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package org.terracotta.management.service;

import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderConfiguration;
import org.terracotta.management.service.impl.DefaultManagementService;
import org.terracotta.voltron.management.ManagementService;

import java.io.IOException;
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
  public void close() throws IOException {
  }
}
