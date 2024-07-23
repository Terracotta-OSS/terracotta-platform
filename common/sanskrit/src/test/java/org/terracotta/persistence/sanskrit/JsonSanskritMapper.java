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
package org.terracotta.persistence.sanskrit;

import org.terracotta.json.DefaultJsonFactory;
import org.terracotta.json.Json;
import org.terracotta.persistence.sanskrit.change.SanskritChange;
import org.terracotta.persistence.sanskrit.change.SanskritChangeVisitor;
import org.terracotta.persistence.sanskrit.json.TestModule;

import java.util.Map;

public class JsonSanskritMapper implements SanskritMapper {

  private final Json json = new DefaultJsonFactory()
      .withModule(new TestModule())
      .create();

  @Override
  public String getCurrentFormatVersion() {
    return "1";
  }

  @Override
  public String toString(SanskritChange change) throws SanskritException {
    MapSanskritVisitor visitor = new MapSanskritVisitor();
    change.accept(visitor);
    try {
      return json.toString(visitor.getMap());
    } catch (RuntimeException e) {
      throw new SanskritException(e);
    }
  }

  @Override
  public void fromString(String src, String version, SanskritChangeVisitor dest) throws SanskritException {
    try {
      set(json.parseObject(src), dest, version);
    } catch (RuntimeException e) {
      throw new SanskritException(e);
    }
  }

  @Override
  public <T> T map(SanskritObject src, Class<T> dest, String version) throws SanskritException {
    MapSanskritVisitor visitor = new MapSanskritVisitor();
    src.accept(visitor);
    try {
      return json.map(visitor.getMap(), dest);
    } catch (RuntimeException e) {
      throw new SanskritException(e);
    }
  }

  private static void set(Map<String, Object> src, SanskritChangeVisitor dest, String version) throws SanskritException {
    for (Map.Entry<String, Object> entry : src.entrySet()) {
      dest.set(entry.getKey(), entry.getValue(), version);
    }
  }
}
