/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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
package org.terracotta.diagnostic.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.diagnostic.common.json.DiagnosticJsonModule;
import org.terracotta.json.Json;

import static java.util.Objects.requireNonNull;

/**
 * @author Mathieu Carbou
 */
public class JsonDiagnosticCodec extends DiagnosticCodecSkeleton<String> {
  private static final Logger LOGGER = LoggerFactory.getLogger(JsonDiagnosticCodec.class);

  private final Json json;

  public JsonDiagnosticCodec(Json.Factory jsonFactory) {
    super(String.class);
    this.json = jsonFactory
        .withModule(new DiagnosticJsonModule())
        .create();
  }

  @Override
  public String serialize(Object o) throws DiagnosticCodecException {
    requireNonNull(o);
    try {
      final String json = this.json.toString(o);
      LOGGER.trace("serialize({}): {}", o, json);
      return json;
    } catch (RuntimeException e) {
      LOGGER.trace("serialize({}): {}", o, e.getMessage(), e);
      throw new DiagnosticCodecException(e);
    }
  }

  @Override
  public <T> T deserialize(String json, Class<T> target) throws DiagnosticCodecException {
    requireNonNull(json);
    requireNonNull(target);
    try {
      final T parsed = this.json.parse(json, target);
      LOGGER.trace("deserialize({}, {}): {}", json, target, parsed);
      return target.cast(parsed);
    } catch (RuntimeException e) {
      LOGGER.trace("deserialize({}, {}): {}", json, target, e.getMessage(), e);
      throw new DiagnosticCodecException(e);
    }
  }

  @Override
  public String toString() {
    return "Json";
  }
}
