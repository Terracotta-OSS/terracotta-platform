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
package org.terracotta.diagnostic.server;

import com.tc.classloader.BuiltinService;
import org.terracotta.diagnostic.server.api.DiagnosticServices;
import org.terracotta.diagnostic.server.api.DiagnosticServicesHolder;
import org.terracotta.entity.PlatformConfiguration;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderConfiguration;

import java.io.Closeable;
import java.util.Collection;
import java.util.Collections;

/**
 * @author Mathieu Carbou
 */
@BuiltinService
public class DiagnosticServiceProvider implements ServiceProvider, Closeable {

  private final DefaultDiagnosticServices diagnosticServices = new DefaultDiagnosticServices();

  @Override
  public boolean initialize(ServiceProviderConfiguration configuration, PlatformConfiguration platformConfiguration) {
    // exposes diagnostic MBean which will make interfaces accessible from clients at this point
    diagnosticServices.init();
    DiagnosticServicesHolder.install(diagnosticServices);
    return true;
  }

  @Override
  public void close() {
    diagnosticServices.close();
  }

  @Override
  public <T> T getService(long consumerID, ServiceConfiguration<T> configuration) {
    if (configuration.getServiceType() == DiagnosticServices.class) {
      return configuration.getServiceType().cast(diagnosticServices);
    }
    throw new UnsupportedOperationException(configuration.getServiceType().getName());
  }

  @Override
  public Collection<Class<?>> getProvidedServiceTypes() {
    return Collections.singletonList(DiagnosticServices.class);
  }

  @Override
  public void prepareForSynchronization() {
    // no-op
  }
}
