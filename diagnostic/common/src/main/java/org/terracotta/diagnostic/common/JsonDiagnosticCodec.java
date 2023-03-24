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
package org.terracotta.diagnostic.common;

import org.terracotta.diagnostic.common.json.DiagnosticJsonModule;
import org.terracotta.json.Json;

import static java.util.Objects.requireNonNull;

/**
 * @author Mathieu Carbou
 */
public class JsonDiagnosticCodec extends DiagnosticCodecSkeleton<String> {

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
      return json.toString(o);
    } catch (RuntimeException e) {
      throw new DiagnosticCodecException(e);
    }
  }

  @Override
  public <T> T deserialize(String json, Class<T> target) throws DiagnosticCodecException {
    requireNonNull(json);
    requireNonNull(target);
    try {
      return target.cast(this.json.parse(json, target));
    } catch (RuntimeException e) {
      throw new DiagnosticCodecException(e);
    }
  }

  @Override
  public String toString() {
    return "Json";
  }
}
