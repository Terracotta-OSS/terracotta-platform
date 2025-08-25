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
package org.terracotta.diagnostic.server.extensions;

import com.tc.classloader.BuiltinService;
import org.terracotta.diagnostic.server.api.extension.DiagnosticExtensions;
import org.terracotta.entity.PlatformConfiguration;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderConfiguration;
import org.terracotta.entity.StateDumpCollector;
import org.terracotta.server.Server;

import java.util.Collection;
import java.util.Collections;

@BuiltinService
public class DiagnosticExtensionsServiceProvider implements ServiceProvider {
  private volatile DiagnosticExtensionsMBeanImpl extensionsMBean;

  @Override
  public boolean initialize(ServiceProviderConfiguration serviceProviderConfiguration, PlatformConfiguration platformConfiguration) {
    Server server = platformConfiguration.getExtendedConfiguration(Server.class).iterator().next();
    extensionsMBean = new DiagnosticExtensionsMBeanImpl(server.getManagement());
    extensionsMBean.expose();
    return true;
  }

  @Override
  public <T> T getService(long l, ServiceConfiguration<T> serviceConfiguration) {
    Class<T> serviceType = serviceConfiguration.getServiceType();
    return serviceType != DiagnosticExtensions.class ? null : serviceType.cast(extensionsMBean);
  }

  @Override
  public Collection<Class<?>> getProvidedServiceTypes() {
    return Collections.singletonList(DiagnosticExtensions.class);
  }

  @Override
  public void prepareForSynchronization() {
  }

  @Override
  public void addStateTo(StateDumpCollector stateDumpCollector) {
    stateDumpCollector.addState("logicalServerState", extensionsMBean.getLogicalServerState());
  }
}
