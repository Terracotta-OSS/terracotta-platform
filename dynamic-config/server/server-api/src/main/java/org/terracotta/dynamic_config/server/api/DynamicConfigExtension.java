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
package org.terracotta.dynamic_config.server.api;

import org.terracotta.entity.PlatformConfiguration;
import org.terracotta.entity.ServiceProviderConfiguration;

import java.util.function.Supplier;

/**
 * Extension to implement as a META-INF/services to provide extensions on the servers
 *
 * @author Mathieu Carbou
 */
public interface DynamicConfigExtension {

  /**
   * Implement this method to add configurations.
   * <p>
   * It is possible to access eagerly some platform information and exposed services to help creating this configurations.
   * Example of services that are available:
   * <ul>
   *   <li>{@link org.terracotta.dynamic_config.api.service.IParameterSubstitutor}: enables the substitution of placeholders</li>
   *   <li>{@link PathResolver}: enables the resolving of relative directories in configurations to the right location.
   *   Before, path were related to the tc-config file.
   *   Now with dynamic-config, this is not the case anymore and a path resolver must be used to appropriately resolve the relative path and resolve placeholders if needed</li>
   *   <li>{@link ConfigChangeHandlerManager}: enabled the registration of dynamic configuration change handlers to validate change requests and eventually apply them at runtime</li>
   * </ul>
   *
   * @param registrar             Implementors may use this object to register their extended configurations or service provider configurations
   * @param platformConfiguration The platform configuration object from where to retrieve useful services that could help creating the configurations
   */
  void configure(Registrar registrar, PlatformConfiguration platformConfiguration);

  interface Registrar {
    /**
     * Register an extended configuration which will only be available when the user asks for a specific class
     */
    default <T> void registerExtendedConfiguration(Class<T> type, T implementation) {
      registerExtendedConfigurationSupplier(type, () -> implementation);
    }

    <T> void registerExtendedConfigurationSupplier(Class<T> type, Supplier<T> implementation);

    /**
     * Register an extended configuration which will be available when the user asks for any super type
     */
    void registerExtendedConfiguration(Object o);

    void registerServiceProviderConfiguration(ServiceProviderConfiguration serviceProviderConfiguration);
  }
}
