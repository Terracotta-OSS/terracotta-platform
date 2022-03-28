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
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.deser.std.FromStringDeserializer;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.inet.InetSocketAddressConverter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * @author Mathieu Carbou
 */
public class Json {

  private static final Logger LOGGER = LoggerFactory.getLogger(Json.class);

  private static final ObjectMapper OBJECT_MAPPER = installSupportedModules(JsonMapper.builder()
      .serializationInclusion(JsonInclude.Include.NON_ABSENT)
      .defaultPropertyInclusion(JsonInclude.Value.construct(JsonInclude.Include.NON_ABSENT, JsonInclude.Include.NON_ABSENT))
      .enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN)
      .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
      .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
      .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
      .enable(SerializationFeature.CLOSE_CLOSEABLE)
      .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
      .configure(SerializationFeature.INDENT_OUTPUT, false)
      .build()
  );

  public static ObjectMapper copyObjectMapper() {
    return OBJECT_MAPPER.copy();
  }

  public static ObjectMapper copyObjectMapper(boolean pretty) {
    return OBJECT_MAPPER.copy().configure(SerializationFeature.INDENT_OUTPUT, pretty);
  }

  public static String toJson(Object o) throws IllegalStateException {
    try {
      return OBJECT_MAPPER.writeValueAsString(o);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }

  public static String toPrettyJson(Object o) throws IllegalStateException {
    try {
      return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(o);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }

  public static JsonNode toJsonTree(Object o) {
    return OBJECT_MAPPER.valueToTree(o);
  }

  public static JsonNode parse(String json) {
    try {
      return OBJECT_MAPPER.readTree(json);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static JsonNode parse(File jsonFile) throws UncheckedIOException {
    try {
      return OBJECT_MAPPER.readTree(jsonFile);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static JsonNode parse(Path jsonFile) throws UncheckedIOException {
    try {
      return OBJECT_MAPPER.readTree(jsonFile.toFile());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static JsonNode parse(URL url) throws UncheckedIOException {
    try {
      return OBJECT_MAPPER.readTree(url);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static JsonNode parse(InputStream stream) throws UncheckedIOException {
    try {
      return OBJECT_MAPPER.readTree(stream);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static JsonNode parse(Reader reader) throws UncheckedIOException {
    try {
      return OBJECT_MAPPER.readTree(reader);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static <T> T parse(String json, TypeReference<T> typeReference) {
    try {
      return OBJECT_MAPPER.readValue(json, typeReference);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static <T> T parse(String json, Class<T> type) {
    try {
      return OBJECT_MAPPER.readValue(json, type);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static <T> T parse(File jsonFile, Class<T> type) throws UncheckedIOException {
    try {
      return OBJECT_MAPPER.readValue(jsonFile, type);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static <T> T parse(Path jsonFile, Class<T> type) throws UncheckedIOException {
    try {
      return OBJECT_MAPPER.readValue(jsonFile.toFile(), type);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static <T> T parse(URL url, Class<T> type) throws UncheckedIOException {
    try {
      return OBJECT_MAPPER.readValue(url, type);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static <T> T parse(InputStream stream, Class<T> type) throws UncheckedIOException {
    try {
      return OBJECT_MAPPER.readValue(stream, type);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static <T> T parse(Reader reader, Class<T> type) throws UncheckedIOException {
    try {
      return OBJECT_MAPPER.readValue(reader, type);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static ObjectMapper installSupportedModules(ObjectMapper objectMapper) {
    objectMapper.registerModule(new TcModule());
    objectMapper.registerModule(new SubtypeDiscoveryModule());
    Stream.of(
        "com.fasterxml.jackson.datatype.jdk8.Jdk8Module",
        "com.fasterxml.jackson.datatype.jsr310.JavaTimeModule")
        .map(cname -> {
          try {
            return Json.class.getClassLoader().loadClass(cname).newInstance();
          } catch (ClassNotFoundException e) {
            LOGGER.warn("Jackson module not found in classpath: " + cname);
            return null;
          } catch (IllegalAccessException | InstantiationException e) {
            throw new AssertionError(e);
          }
        })
        .filter(Objects::nonNull)
        .map(Module.class::cast)
        .forEach(objectMapper::registerModule);
    return objectMapper;
  }

  private static class TcModule extends SimpleModule {
    private static final long serialVersionUID = 1L;

    TcModule() {
      super("TcModule", new Version(1, 0, 0, null, null, null));
      addSerializer(Path.class, ToStringSerializer.instance);
      addSerializer(InetSocketAddress.class, ToStringSerializer.instance);
      addDeserializer(Path.class, new FromStringDeserializer<Path>(Path.class) {
        private static final long serialVersionUID = 1L;

        @Override
        protected Path _deserialize(String value, DeserializationContext ctxt) {
          return Paths.get(value);
        }
      });
      addDeserializer(InetSocketAddress.class, new FromStringDeserializer<InetSocketAddress>(InetSocketAddress.class) {
        private static final long serialVersionUID = 1L;

        @Override
        protected InetSocketAddress _deserialize(String value, DeserializationContext ctxt) {
          return InetSocketAddressConverter.getInetSocketAddress(value);
        }
      });
    }
  }

  private static class SubtypeDiscoveryModule extends SimpleModule {
    private static final long serialVersionUID = 1L;

    SubtypeDiscoveryModule() {
      super("SubtypeDiscoveryModule", new Version(1, 0, 0, null, null, null));
      try (ScanResult scanResult = new ClassGraph()
          .enableClassInfo()
          .enableAnnotationInfo()
          .whitelistPackages("org.terracotta")
          .scan()) {
        for (ClassInfo classInfo : scanResult.getClassesWithAnnotation(JsonTypeName.class.getName())) {
          final Class<?> clazz = classInfo.loadClass(true);
          if (clazz == null) {
            LOGGER.warn("Unable to register class: {} as a Json subtype", classInfo.getName());
          } else {
            registerSubtypes(clazz);
            LOGGER.debug("Registered class: {} as a Json subtype", classInfo.getName());
          }
        }
      }
    }
  }
}
