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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.impl.NullsConstantProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Deque;

import static java.util.Arrays.asList;

/**
 * @author Mathieu Carbou
 */
public class TerracottaJsonModule extends SimpleModule implements Json.Module, DefaultJsonFactory.JacksonModule {
  private static final long serialVersionUID = 1L;

  @Override
  public void configure(ObjectMapper objectMapper) {
    objectMapper.setTypeFactory(TypeFactory.defaultInstance().withClassLoader(getClass().getClassLoader()))
        .setSerializationInclusion(JsonInclude.Include.NON_ABSENT)
        .setDefaultPropertyInclusion(JsonInclude.Value.construct(JsonInclude.Include.NON_ABSENT, JsonInclude.Include.NON_ABSENT))
        .enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
        .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
        .enable(SerializationFeature.CLOSE_CLOSEABLE)
        .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
        // setting FAIL_ON_UNKNOWN_PROPERTIES to false will help backward compatibility to ignore
        // some json fields that are present in the input message if they are not needed when deserializing
        // and mapping to an object. This does not mean that it will achieve complete backward compat, but
        // it will prevent Jackson from failing when it sees a json input that cannot be mapped to a field in
        // a target object
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  @Override
  public Iterable<? extends Module> getDependencies() {
    return asList(new Jdk8Module(), new JavaTimeModule());
  }

  public TerracottaJsonModule() {
    super(TerracottaJsonModule.class.getSimpleName(), new Version(1, 0, 0, null, null, null));

    addSerializer(Json.Null.class, new StdSerializer<Json.Null>(Json.Null.class) {
      private static final long serialVersionUID = 1L;

      @Override
      public void serialize(Json.Null value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeNull();
      }
    });

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
