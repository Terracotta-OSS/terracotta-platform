/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2026
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
package org.terracotta.security.logger;

import com.tc.classloader.BuiltinService;
import com.tc.spi.Guardian;
import org.terracotta.entity.PlatformConfiguration;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderConfiguration;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.stream.Collectors.joining;

@BuiltinService
public class SecurityLoggerServiceProvider implements ServiceProvider {
  private volatile PlatformConfiguration platformConfiguration;

  @Override
  public boolean initialize(ServiceProviderConfiguration configuration, PlatformConfiguration platformConfiguration) {
    this.platformConfiguration = platformConfiguration;
    return true;
  }

  @Override
  public <T> T getService(long consumerID, ServiceConfiguration<T> configuration) {
    // provides SecurityLogger interface to other plugins
    if (configuration.getServiceType() == SecurityLogger.class) {
      return configuration.getServiceType().cast(findSecurityLogger().orElse(SecurityLogger.NOOP));
    }

    // provide Guardian implementation to core
    if (consumerID == 0 && configuration.getServiceType() == Guardian.class) {
      return configuration.getServiceType().cast(new Guardian() {
        private final AtomicReference<SecurityLogger> cachedLogger = new AtomicReference<>();

        @Override
        public boolean validate(Op op, Properties properties) {
          if (op == Op.SECURITY_OP) {
            Object message = properties.remove("id");
            if (message != null) {
              SecurityLogger securityLogger = this.cachedLogger.get();
              // The following code within the if block is just to trigger a new search for the logger until it is found.
              // It will be found later once SecurityLoggerExtension is loaded and has registered a security logger implementation
              if (securityLogger == null) {
                securityLogger = findSecurityLogger()
                  .filter(logger -> this.cachedLogger.compareAndSet(null, logger))
                  .orElseGet(this.cachedLogger::get);
              }
              // securityLogger will be null until SecurityLoggerExtension is loaded and has registered a security logger implementation
              if (securityLogger != null) {
                String context = properties.stringPropertyNames().stream().sorted().map(key -> key + "=" + properties.getProperty(key)).collect(joining(", "));
                if (context.isEmpty()) {
                  securityLogger.log(message.toString());
                } else {
                  securityLogger.log("{}: {}", message, context);
                }
              }
            }
          }
          return true;
        }
      });
    }

    throw new UnsupportedOperationException(configuration.getServiceType().getName());
  }

  @Override
  public Collection<Class<?>> getProvidedServiceTypes() {
    return List.of(SecurityLogger.class, Guardian.class);
  }

  @Override
  public void prepareForSynchronization() {
    // no-op
  }

  private Optional<SecurityLogger> findSecurityLogger() {
    Collection<SecurityLogger> loggers = platformConfiguration.getExtendedConfiguration(SecurityLogger.class);
    if (loggers.size() != 1) {
      throw new IllegalStateException("Multiple instances found for " + SecurityLogger.class);
    }
    return loggers.stream().findAny();
  }
}
