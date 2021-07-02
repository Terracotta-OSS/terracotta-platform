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
package org.terracotta.diagnostic.server.extensions;

import com.tc.classloader.BuiltinService;
import com.tc.productinfo.BuildInfo;
import com.tc.productinfo.Description;
import org.terracotta.diagnostic.server.api.extension.DiagnosticExtensions;
import org.terracotta.entity.PlatformConfiguration;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderConfiguration;
import org.terracotta.entity.StateDumpCollector;
import org.terracotta.server.Server;

import java.util.Collection;
import java.util.Collections;
import java.util.ServiceLoader;
import java.util.stream.StreamSupport;

@BuiltinService
public class DiagnosticExtensionsServiceProvider implements ServiceProvider {
  private volatile DiagnosticExtensionsMBeanImpl logicalServerStateMBean;

  @Override
  public boolean initialize(ServiceProviderConfiguration serviceProviderConfiguration, PlatformConfiguration platformConfiguration) {
    Server server = platformConfiguration.getExtendedConfiguration(Server.class).iterator().next();

    ClassLoader classLoader = server.getServiceClassLoader(ServiceProvider.class.getClassLoader(), Description.class);
    ServiceLoader<Description> serviceLoader = ServiceLoader.load(Description.class, classLoader);
    BuildInfo buildInfo = StreamSupport.stream(serviceLoader.spliterator(), false)
        .filter(BuildInfo.class::isInstance)
        .map(BuildInfo.class::cast)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("No BuildInfo found"));

    logicalServerStateMBean = new DiagnosticExtensionsMBeanImpl(server.getManagement(), buildInfo);
    logicalServerStateMBean.expose();
    return true;
  }

  @Override
  public <T> T getService(long l, ServiceConfiguration<T> serviceConfiguration) {
    Class<T> serviceType = serviceConfiguration.getServiceType();
    return serviceType != DiagnosticExtensions.class ? null : serviceType.cast(logicalServerStateMBean);
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
    stateDumpCollector.addState("logicalServerState", logicalServerStateMBean.getLogicalServerState());
  }
}
