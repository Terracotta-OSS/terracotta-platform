/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
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
