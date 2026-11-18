/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2026
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law ord agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.platform.metrics.voltron;

import java.util.Collection;
import java.util.Map;

/**
 *
 */
public class InvokeStateHelper {
  private final Map<String, ?> state;


  public InvokeStateHelper(Map<String, ?> state) {
    this.state = state;
  }

  public long getTotalCount() {
    long total = 0;
    @SuppressWarnings("unchecked")
    Collection<Map<String, ?>> entities = (Collection<Map<String, ?>>)state.get("entities");
    for (Map<String, ?> e : entities) {
      Object count = e.get("invokeCount");
      if (count instanceof Long c) total += c;
    }
    return total;
  }

  public long getTypeCount(String type) {
    long total = 0;
    @SuppressWarnings("unchecked")
    Collection<Map<String, ?>> entities = (Collection<Map<String, ?>>)state.get("entities");
    for (Map<String, ?> e : entities) {
      String typeCheck = String.valueOf(e.get("type"));
      Object count = e.get("invokeCount");
      if (typeCheck.equals(type) && count instanceof Long c) total += c;
    }
    return total;
  }
}
