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
package org.terracotta.persistence.sanskrit.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.terracotta.json.Json;
import org.terracotta.persistence.sanskrit.MapSanskritVisitor;

import java.io.IOException;

/**
 * @author Mathieu Carbou
 */
public class SanskritJsonModule extends SimpleModule implements Json.Module {
  private static final long serialVersionUID = 1L;

  public SanskritJsonModule() {
    super(SanskritJsonModule.class.getSimpleName(), new Version(1, 0, 0, null, null, null));
    addSerializer(MapSanskritVisitor.Null.class, new StdSerializer<MapSanskritVisitor.Null>(MapSanskritVisitor.Null.class) {
      private static final long serialVersionUID = 1L;

      @Override
      public void serialize(MapSanskritVisitor.Null value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeNull();
      }
    });
  }
}
