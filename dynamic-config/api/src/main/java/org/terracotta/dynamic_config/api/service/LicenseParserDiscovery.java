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
public class LicenseParserDiscovery {

  private final ClassLoader classLoader;

  public LicenseParserDiscovery() {
    this(LicenseParserDiscovery.class.getClassLoader());
  }

  public LicenseParserDiscovery(ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  public Optional<LicenseParser> find() {
    List<LicenseParser> services = StreamSupport.stream(ServiceLoader.load(LicenseParser.class, classLoader).spliterator(), false).collect(Collectors.toList());
    if (services.isEmpty()) {
      return Optional.empty();
    }
    if (services.size() == 1) {
      return Optional.of(services.get(0));
    }
    throw new IllegalStateException("Found several implementations of " + LicenseParser.class.getName() + " on classpath");
  }
}
