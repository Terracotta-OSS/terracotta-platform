/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.system_tests.util;

import java.util.Map;
import java.util.Properties;

import static java.util.stream.Collectors.toMap;

/**
 * @author Mathieu Carbou
 */
public class PropertyResolver {
  private final Map<String, String> variables;

  public PropertyResolver(Properties variables) {
    this.variables = variables.entrySet().stream().collect(toMap(e -> "${" + e.getKey() + "}", e -> String.valueOf(e.getValue())));
  }

  public String resolve(String str) {
    return str == null ? null : variables.entrySet().stream().reduce(str, (s, e) -> s.replace(e.getKey(), e.getValue()), (s1, s2) -> {
      throw new UnsupportedOperationException();
    });
  }

  public Properties resolveAll(Properties p) {
    return p.entrySet().stream().reduce(new Properties(), (out, e) -> {
      out.setProperty(String.valueOf(e.getKey()), e.getValue() == null ? null : resolve(String.valueOf(e.getValue())));
      return out;
    }, (p1, p2) -> {
      throw new UnsupportedOperationException();
    });
  }
}
