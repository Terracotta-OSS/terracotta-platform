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
package org.terracotta.dynamic_config.api.service;

import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * @author Mathieu Carbou
 */
public class ConfigRepositoryMapperDiscovery {

  private final PathResolver pathResolver;
  private final ClassLoader classLoader;

  public ConfigRepositoryMapperDiscovery(PathResolver pathResolver) {
    this(pathResolver, ConfigRepositoryMapperDiscovery.class.getClassLoader());
  }

  public ConfigRepositoryMapperDiscovery(PathResolver pathResolver, ClassLoader classLoader) {
    this.pathResolver = pathResolver;
    this.classLoader = classLoader;
  }

  public Optional<ConfigRepositoryMapper> find() {
    List<ConfigRepositoryMapper> services = StreamSupport.stream(ServiceLoader.load(ConfigRepositoryMapper.class, classLoader).spliterator(), false).collect(Collectors.toList());
    if (services.isEmpty()) {
      return Optional.empty();
    }
    if (services.size() == 1) {
      ConfigRepositoryMapper service = services.get(0);
      service.init(pathResolver);
      return Optional.of(service);
    }
    throw new IllegalStateException("Found several implementations of " + ConfigRepositoryMapper.class.getName() + " on classpath");
  }
}
