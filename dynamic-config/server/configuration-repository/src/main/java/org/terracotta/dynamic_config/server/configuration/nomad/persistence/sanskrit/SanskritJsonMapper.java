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
package org.terracotta.dynamic_config.server.configuration.nomad.persistence.sanskrit;

import org.terracotta.dynamic_config.api.model.Version;
import org.terracotta.json.Json;
import org.terracotta.persistence.sanskrit.SanskritException;
import org.terracotta.persistence.sanskrit.SanskritMapper;
import org.terracotta.persistence.sanskrit.SanskritObject;
import org.terracotta.persistence.sanskrit.change.SanskritChange;
import org.terracotta.persistence.sanskrit.change.SanskritChangeVisitor;

import java.util.HashMap;
import java.util.Map;

public class SanskritJsonMapper implements SanskritMapper {

  private final Map<String, Json> mappers = new HashMap<>();

  public SanskritJsonMapper(Json.Factory jsonFactory) {
    Json json = jsonFactory.pretty(false).create();
    Json jsonV1 = createDeprecatedV1Mapper(jsonFactory);
    mappers.put("", jsonV1);
    mappers.put(Version.V1.getValue(), jsonV1);
    mappers.put(Version.V2.getValue(), json);
    mappers.put(getCurrentFormatVersion(), json);
  }

  @Override
  public String getCurrentFormatVersion() {
    return Version.CURRENT.getValue();
  }

  @Override
  public String toString(SanskritChange change) throws SanskritException {
    SanskritMapVisitor visitor = new SanskritMapVisitor();
    change.accept(visitor);
    try {
      return getJson(getCurrentFormatVersion()).toString(visitor.getMap());
    } catch (RuntimeException e) {
      throw new SanskritException(e);
    }
  }

  @Override
  public void fromString(String src, String version, SanskritChangeVisitor dest) throws SanskritException {
    try {
      set(getJson(version).parseObject(src), dest, version);
    } catch (RuntimeException e) {
      throw new SanskritException(e);
    }
  }

  @Override
  public <T> T map(SanskritObject src, Class<T> dest, String version) throws SanskritException {
    SanskritMapVisitor visitor = new SanskritMapVisitor();
    src.accept(visitor);
    try {
      return getJson(version).map(visitor.getMap(), dest);
    } catch (RuntimeException e) {
      throw new SanskritException(e);
    }
  }

  private Json getJson(String version) {
    final String v = version == null ? getCurrentFormatVersion() : version;
    final Json json = mappers.get(v);
    if (json == null) {
      throw new IllegalArgumentException("Unsupported version: " + v);
    }
    return json;
  }

  private static void set(Map<String, Object> src, SanskritChangeVisitor dest, String version) throws SanskritException {
    for (Map.Entry<String, Object> entry : src.entrySet()) {
      dest.set(entry.getKey(), entry.getValue(), version);
    }
  }

  @SuppressWarnings("deprecation")
  private static Json createDeprecatedV1Mapper(Json.Factory jsonFactory) {
    return jsonFactory
        .withModule(new org.terracotta.dynamic_config.api.json.DynamicConfigJsonModuleV1())
        .create();
  }
}
