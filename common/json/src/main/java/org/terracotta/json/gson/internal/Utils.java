/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
package org.terracotta.json.gson.internal;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.TreeSet;

public class Utils {
  public static JsonElement sort(JsonElement jsonElement) {
    if (jsonElement.isJsonObject() && jsonElement.getAsJsonObject().size() > 1) {
      final JsonObject copy = new JsonObject();
      final JsonObject original = jsonElement.getAsJsonObject();
      for (String key : new TreeSet<>(original.keySet())) {
        copy.add(key, sort(original.get(key)));
      }
      return copy;
    }
    return jsonElement;
  }
}
