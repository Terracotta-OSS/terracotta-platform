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

import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * @author Mathieu Carbou
 */
public class LicenseParserDiscovery {

  private final ClassLoader classLoader;

  public LicenseParserDiscovery(ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  public Optional<LicenseService> find() {
    List<LicenseService> services = StreamSupport.stream(ServiceLoader.load(LicenseService.class, classLoader).spliterator(), false).collect(Collectors.toList());
    if (services.isEmpty()) {
      return Optional.empty();
    }
    if (services.size() == 1) {
      return Optional.of(services.get(0));
    }
    throw new IllegalStateException("Found several implementations of " + LicenseService.class.getName() + " on classpath");
  }
}
