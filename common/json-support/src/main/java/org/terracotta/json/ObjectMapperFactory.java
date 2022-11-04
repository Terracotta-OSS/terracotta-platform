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
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

/**
 * Used to build an ObjectMapper in a consistent way
 *
 * @author Mathieu Carbou
 */
public class ObjectMapperFactory {

  private final boolean pretty;
  private final List<Module> modules;
  private final String eol;

  public ObjectMapperFactory() {
    this(emptyList(), false, "\n");
  }

  private ObjectMapperFactory(List<Module> modules, boolean pretty, String eol) {
    this.pretty = pretty;
    this.modules = new ArrayList<>(modules);
    this.eol = eol;
  }

  public ObjectMapperFactory pretty() {
    return pretty(true);
  }

  public ObjectMapperFactory eol(String eol) {
    return new ObjectMapperFactory(this.modules, this.pretty, eol);
  }

  public ObjectMapperFactory systemEOL() {
    return eol(System.lineSeparator());
  }

  public ObjectMapperFactory pretty(boolean pretty) {
    return new ObjectMapperFactory(this.modules, pretty, this.eol);
  }

  public ObjectMapperFactory withModule(Module module) {
    return withModules(module);
  }

  public ObjectMapperFactory withModules(Module... modules) {
    ObjectMapperFactory factory = new ObjectMapperFactory(this.modules, this.pretty, this.eol);
    factory.modules.addAll(asList(modules));
    return factory;
  }

  public ObjectMapper create() {
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
    for (Module module : modules) {
      mapper.registerModule(module);
    }
    return mapper;
  }

  @Override
  public String toString() {
    return "ObjectMapperFactory{" +
        "pretty=" + pretty +
        ", modules=" + modules +
        '}';
  }
}
