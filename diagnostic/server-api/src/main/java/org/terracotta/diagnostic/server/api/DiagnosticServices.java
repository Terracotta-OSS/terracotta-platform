/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
package org.terracotta.diagnostic.server.api;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

/**
 * @author Mathieu Carbou
 */
public interface DiagnosticServices {
  /**
   * List diagnostic services
   */
  Collection<Class<?>> listServices();

  /**
   * Tries to find a diagnostic service
   */
  <T> Optional<T> findService(Class<T> serviceInterface);

  /**
   * Returns a completion stage that will bec completed once a service is made or is available
   */
  <T> CompletionStage<T> onService(Class<T> serviceInterface);

  /**
   * Register an action to execute once a service is made available
   */
  <T> void onService(Class<T> serviceInterface, Consumer<T> action);

  /**
   * Clears the registrations
   */
  void clear();

  /**
   * Exposes a service through diagnostic port
   */
  <T> DiagnosticServicesRegistration<T> register(Class<T> serviceInterface, T serviceImplementation);

  /**
   * Unregister a service
   */
  <T> void unregister(Class<T> serviceInterface);
}
