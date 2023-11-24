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

import java.util.Collection;
import java.util.function.Supplier;

/**
 * Extension to implement as a META-INF/services to provide extensions on the servers
 *
 * @author Mathieu Carbou
 */
public interface DynamicConfigExtension {

  /**
   * @return true if this extension can only be loaded when a node is starting from an already created configuration folder.
   * If the node is starting in diagnostic mode (for repair or initially) then the extension won't be loaded except if this method returns false.
   */
  default boolean onlyWhenNodeConfigured() { return true; }

  /**
   * Implement this method to add configurations.
   * <p>
   * It is possible to access eagerly some platform information and exposed services to help creating these configurations.
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

  default <T> T findService(PlatformConfiguration platformConfiguration, Class<T> type) {
    Collection<T> services = platformConfiguration.getExtendedConfiguration(type);
    if (services.isEmpty()) {
      throw new AssertionError("No instance of service " + type + " found");
    }

    if (services.size() == 1) {
      T instance = services.iterator().next();
      if (instance == null) {
        throw new AssertionError("Instance of service " + type + " found to be null");
      }
      return instance;
    }
    throw new AssertionError("Multiple instances of service " + type + " found");
  }

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
