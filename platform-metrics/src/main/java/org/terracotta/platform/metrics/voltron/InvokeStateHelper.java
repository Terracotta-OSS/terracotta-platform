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
    Object count = decend(this.state, new String[] {"invokeRuntimes","TOTAL", "count"});
    if (count instanceof Long c) return c;
    return 0L;
  }

  public long getTypeCount(String type) {
    Object count = decend(this.state, new String[] {"typeInvokeRuntimes",type,"TOTAL", "count"});
    if (count instanceof Long c) return c;
    return 0L;
  }

  @SuppressWarnings("unchecked")
  private static Object decend(Map<String, ?> target, String[] path) {
    Object prospect = target;
    for (String key : path) {
      if (prospect instanceof Map m) {
        target = (Map<String, Object>)m;
      } else {
        return null;
      }
      prospect = target.get(key);
    }
    return prospect;
  }
}
