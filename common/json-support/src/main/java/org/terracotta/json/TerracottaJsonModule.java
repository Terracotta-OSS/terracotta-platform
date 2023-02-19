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
package org.terracotta.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.impl.NullsConstantProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * @author Mathieu Carbou
 */
public class TerracottaJsonModule extends SimpleModule {
  private static final long serialVersionUID = 1L;

  public TerracottaJsonModule() {
    super(TerracottaJsonModule.class.getSimpleName(), new Version(1, 0, 0, null, null, null));
    addSerializer(Path.class, new StdSerializer<Path>(Path.class) {
      private static final long serialVersionUID = 1L;

      @Override
      public void serialize(Path value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartArray();
        for (Path segment : value) {
          gen.writeString(segment.toString());
        }
        gen.writeEndArray();
      }
    });
    addDeserializer(Path.class, new StdDeserializer<Path>(Path.class) {
      private static final long serialVersionUID = 1L;

      @Override
      public Path deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        Deque<String> segments = new ArrayDeque<>();
        try {
          if (p.getCurrentToken() == JsonToken.VALUE_STRING) {
            // backward compat with V1
            segments.add(p.getText());
          } else {
            // new parsing, using arrays
            while (true) {
              // optimization for collections of strings
              String value = p.nextTextValue();
              if (value != null) {
                segments.add(value);
              } else if (p.getCurrentToken() == JsonToken.END_ARRAY) {
                break;
              } else {
                value = _parseString(p, ctxt, NullsConstantProvider.nuller());
                segments.add(value);
              }
            }
          }
        } catch (Exception e) {
          throw JsonMappingException.wrapWithPath(e, segments, segments.size());
        }
        String first = segments.removeFirst();
        String[] others = segments.toArray(new String[0]);
        return Paths.get(first, others);
      }
    });
  }
}
