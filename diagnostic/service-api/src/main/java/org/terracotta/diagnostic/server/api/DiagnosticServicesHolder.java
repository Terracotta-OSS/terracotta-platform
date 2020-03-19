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
package org.terracotta.diagnostic.server.api;

import java.util.concurrent.CompletableFuture;

/**
 * @author Mathieu Carbou
 */
public class DiagnosticServicesHolder {
  private static final CompletableFuture<DiagnosticServices> SERVICE = new CompletableFuture<>();

  public static synchronized void install(DiagnosticServices service) {
    if (SERVICE.isDone()) {
      throw new IllegalStateException(DiagnosticServices.class.getSimpleName() + " already installed");
    }
    SERVICE.complete(service);
  }

  public static <T> void willRegister(Class<T> serviceInterface, T serviceImplementation) {
    SERVICE.thenAccept(diagnosticServices -> diagnosticServices.register(serviceInterface, serviceImplementation));
  }
}
