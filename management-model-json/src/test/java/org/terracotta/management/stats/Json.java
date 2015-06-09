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
package org.terracotta.management.stats;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.terracotta.management.stats.jackson.MixIns;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

/**
 * @author Ludovic Orban
 */
class Json {

  private static final ObjectMapper mapper = new ObjectMapper();

  static {
    Map<Class<?>, Class<?>> mixins = MixIns.mixins();
    for (Map.Entry<Class<?>, Class<?>> entry : mixins.entrySet()) {
      mapper.addMixIn(entry.getKey(), entry.getValue());
    }
  }

  public static String toJson(Object o) throws IOException {
    StringWriter stringWriter = new StringWriter();
    mapper.writeValue(stringWriter, o);
    return stringWriter.toString();
  }

  public static <T> T fromJson(String json) throws IOException {
    return (T) fromJson(json, Statistic.class);
  }

  public static <T> T fromJson(String json, Class<T> clazz) throws IOException {
    return mapper.readValue(json, clazz);
  }
}
