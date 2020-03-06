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
package org.terracotta.dynamic_config.test_support.util;

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
