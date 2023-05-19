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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

/**
 * Used to build an ObjectMapper in a consistent way
 *
 * @author Mathieu Carbou
 */
public class DefaultJsonFactory implements Json.Factory {

  private final boolean pretty;
  private final List<Json.Module> modules;
  private final String eol;

  public DefaultJsonFactory() {
    this(emptyList(), false, "\n");
  }

  private DefaultJsonFactory(List<Json.Module> modules, boolean pretty, String eol) {
    this.pretty = pretty;
    this.modules = new ArrayList<>(modules);
    this.eol = eol;
  }

  @Override
  public Json create() {
    return new JacksonJson(createObjectMapper());
  }

  @Override
  public DefaultJsonFactory pretty() {
    return pretty(true);
  }

  @Override
  public DefaultJsonFactory eol(String eol) {
    return new DefaultJsonFactory(this.modules, this.pretty, eol);
  }

  @Override
  public DefaultJsonFactory systemEOL() {
    return eol(System.lineSeparator());
  }

  @Override
  public DefaultJsonFactory pretty(boolean pretty) {
    return new DefaultJsonFactory(this.modules, pretty, this.eol);
  }

  @Override
  public DefaultJsonFactory withModule(Json.Module module) {
    return withModules(module);
  }

  @Override
  public DefaultJsonFactory withModules(Json.Module... modules) {
    DefaultJsonFactory factory = new DefaultJsonFactory(this.modules, this.pretty, this.eol);
    factory.modules.addAll(asList(modules));
    return factory;
  }

  public ObjectMapper createObjectMapper() {
    ObjectMapper mapper = JsonMapper.builder()
        .typeFactory(TypeFactory.defaultInstance().withClassLoader(getClass().getClassLoader()))
        .serializationInclusion(JsonInclude.Include.NON_ABSENT)
        .defaultPropertyInclusion(JsonInclude.Value.construct(JsonInclude.Include.NON_ABSENT, JsonInclude.Include.NON_ABSENT))
        .enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN)
        .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
        .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
        .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
        .enable(SerializationFeature.CLOSE_CLOSEABLE)
        .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
        .configure(SerializationFeature.INDENT_OUTPUT, pretty)
        // setting FAIL_ON_UNKNOWN_PROPERTIES to false will help backward compatibility to ignore
        // some json fields that are present in the input message if they are not needed when deserializing
        // and mapping to an object. This does not mean that it will achieve complete backward compat, but
        // it will prevent Jackson from failing when it sees a json input that cannot be mapped to a field in
        // a target object
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .build();
    if (pretty) {
      DefaultIndenter indent = new DefaultIndenter("  ", eol);
      mapper.writer(new DefaultPrettyPrinter()
          .withObjectIndenter(indent)
          .withArrayIndenter(indent));
    }
    // modules need to be registered first for dependency resolution
    for (Json.Module module : modules) {
      if (module instanceof Module) {
        mapper.registerModule((Module) module);
      }
    }
    // then they can be configured
    for (Json.Module module : modules) {
      if (module instanceof JacksonModule) {
        ((JacksonModule) module).configure(mapper);
      }
    }
    return mapper;
  }

  @Override
  public String toString() {
    return "ObjectMapperFactory{" +
        ", eol=" + eol.replace("\n", "\\n").replace("\r", "\\r") +
        ", pretty=" + pretty +
        ", modules=" + modules +
        '}';
  }

  public interface JacksonModule extends Json.Module {
    void configure(ObjectMapper objectMapper);
  }

  protected static class JacksonJson implements Json {
    private final ObjectMapper mapper;

    protected JacksonJson(ObjectMapper mapper) {
      this.mapper = mapper;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> parseObject(String json) {
      try {
        return mapper.readValue(json, Map.class);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> parseObject(Reader r) {
      try {
        return mapper.readValue(r, Map.class);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Object> parseList(Path path) {
      try {
        return mapper.readValue(path.toFile(), List.class);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Object> parseList(String json) {
      try {
        return mapper.readValue(json, List.class);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Object> parseList(Reader json) {
      try {
        return mapper.readValue(json, List.class);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    @Override
    public <T> T parse(String json, Class<T> type) {
      try {
        return mapper.readValue(json, type);
      } catch (JsonProcessingException e) {
        throw new UncheckedIOException(e);
      }
    }

    @Override
    public Object parse(String json, Type type) {
      try {
        return mapper.readValue(json, mapper.constructType(type));
      } catch (JsonProcessingException e) {
        throw new UncheckedIOException(e);
      }
    }

    @Override
    public <T> T parse(Path path, Class<T> type) {
      try {
        return mapper.readValue(path.toFile(), type);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    @Override
    public Object parse(Path path, Type type) {
      try {
        return mapper.readValue(path.toFile(), mapper.constructType(type));
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    @Override
    public <T> T parse(Reader r, Class<T> type) {
      try {
        return mapper.readValue(r, type);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    @Override
    public Object parse(Reader r, Type type) {
      try {
        return mapper.readValue(r, mapper.constructType(type));
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> parseObject(Path path) {
      try {
        return mapper.readValue(path.toFile(), Map.class);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    @Override
    public String toString(Object o, boolean pretty) {
      try {
        return pretty ? mapper.writerWithDefaultPrettyPrinter().writeValueAsString(o) : mapper.writeValueAsString(o);
      } catch (JsonProcessingException e) {
        throw new UncheckedIOException(e);
      }
    }
  }
}
