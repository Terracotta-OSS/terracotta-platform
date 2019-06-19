/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli;

import javax.annotation.Resource;
import java.util.stream.Stream;

/**
 * @author Mathieu Carbou
 */
public class Injector {
  public static <T> T inject(T target, Object... services) {
    Stream.of(target.getClass().getFields())
        .filter(field -> field.isAnnotationPresent(Resource.class))
        .forEach(field -> {
          Object found = Stream.of(services)
              .filter(service -> field.getType().isInstance(service))
              .findFirst()
              .orElseThrow(() -> new IllegalStateException("No service found to inject into " + field));
          try {
            field.set(target, found);
          } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
          }
        });
    return target;
  }
}
