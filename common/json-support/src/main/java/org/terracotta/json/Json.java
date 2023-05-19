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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Terracotta Json
 *
 * @author Mathieu Carbou
 */
public interface Json {

  /**
   * Serialize the object and then parses back the serialized json.
   * This is a way to map a complex Java object into Map, List, Number, String, etc
   */
  default Object map(Object o) {
    return map(o, Object.class);
  }

  /**
   * Serialize the object and then parses back the serialized json to a given type.
   * This is a way to map a complex Java object into another.
   */
  default <T> T map(Object o, Class<T> type) {
    return parse(toString(o), type);
  }

  default Object map(Object o, Type type) {
    return parse(toString(o), type);
  }

  /**
   * Serialize the object and then parses back the serialized json to a Map containing
   * Map, List, Number, String, etc
   */
  default Map<String, Object> mapToObject(Object o) {
    return parseObject(toString(o));
  }

  /**
   * Serialize the object and then parses back the serialized json to a List containing
   * Map, List, Number, String, etc
   */
  default List<Object> mapToList(Object o) {
    return parseList(toString(o));
  }

  /**
   * Parses a json content into an unknown structure (Map, List, Number, String)
   */
  default Object parse(String json) {
    return parse(json, Object.class);
  }

  /**
   * Parses a json content in a file into an unknown structure (Map, List, Number, String)
   */
  default Object parse(File file) {
    return parse(file, Object.class);
  }

  /**
   * Parses a json content in a file into an unknown structure (Map, List, Number, String)
   */
  default Object parse(Path path) {
    return parse(path, Object.class);
  }

  /**
   * Parses a json content into an unknown structure (Map, List, Number, String)
   */
  default Object parse(InputStream json) {
    return parse(json, Object.class);
  }

  /**
   * Parses a json content into an unknown structure (Map, List, Number, String)
   */
  default Object parse(Reader json) {
    return parse(json, Object.class);
  }

  /**
   * Parses a json content into an unknown structure (Map, List, Number, String)
   */
  default Object parse(URL url) {
    return parse(url, Object.class);
  }

  /**
   * Parses a json content (object expected) in a file into a Map structure
   */
  default Map<String, Object> parseObject(File file) {
    return parseObject(file.toPath());
  }

  /**
   * Parses a json content (object expected) in a file into a Map structure
   */
  Map<String, Object> parseObject(Path path);

  /**
   * Parses a json content (object expected) into a Map structure
   */
  Map<String, Object> parseObject(String json);

  /**
   * Parses a json content (object expected) into a Map structure
   */
  default Map<String, Object> parseObject(InputStream json) {
    return parseObject(new InputStreamReader(json, UTF_8));
  }

  /**
   * Parses a json content (object expected) into a Map structure
   */
  default Map<String, Object> parseObject(URL url) {
    try (InputStream is = url.openStream()) {
      return parseObject(is);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Parses a json content (object expected) into a Map structure
   */
  Map<String, Object> parseObject(Reader r);

  /**
   * Parses a json content (list expected) in a file into a List structure
   */
  default List<Object> parseList(File file) {
    return parseList(file.toPath());
  }

  List<Object> parseList(Path path);

  /**
   * Parses a json content (list expected) into a List structure
   */
  List<Object> parseList(String json);

  /**
   * Parses a json content (list expected) into a List structure
   */
  default List<Object> parseList(InputStream json) {
    return parseList(new InputStreamReader(json, UTF_8));
  }

  /**
   * Parses a json content (list expected) into a List structure
   */
  default List<Object> parseList(URL url) {
    try (InputStream is = url.openStream()) {
      return parseList(is);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Parses a json content (list expected) into a List structure
   */
  List<Object> parseList(Reader json);

  /**
   * Parses a json content into a Java model
   */
  <T> T parse(String json, Class<T> type);

  Object parse(String json, Type type);

  /**
   * Parses a json content in a file into a Java model
   */
  default <T> T parse(File file, Class<T> type) {
    return parse(file.toPath(), type);
  }

  default Object parse(File file, Type type) {
    return parse(file.toPath(), type);
  }

  /**
   * Parses a json content in a file into a Java model
   */
  <T> T parse(Path path, Class<T> type);

  Object parse(Path path, Type type);

  /**
   * Parses a json content from a stream into a Java model
   */
  default <T> T parse(InputStream is, Class<T> type) {
    return parse(new InputStreamReader(is, UTF_8), type);
  }

  default Object parse(InputStream is, Type type) {
    return parse(new InputStreamReader(is, UTF_8), type);
  }

  /**
   * Parses a json content from a stream into a Java model
   */
  default <T> T parse(URL url, Class<T> type) {
    try (InputStream is = url.openStream()) {
      return parse(is, type);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  default Object parse(URL url, Type type) {
    try (InputStream is = url.openStream()) {
      return parse(is, type);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Parses a json content from a stream into a Java model
   */
  <T> T parse(Reader r, Class<T> type);

  Object parse(Reader r, Type type);

  String toString(Object o, boolean pretty);

  /**
   * Serialize an object into a Json string
   */
  default String toString(Object o) {
    return toString(o, false);
  }

  /**
   * Serialize an object into a pretty Json string
   */
  default String toPrettyString(Object o) {
    return toString(o, true);
  }

  /**
   * Serialize an object into Json in a file
   */
  default void write(Object o, File out, boolean pretty) {
    write(o, out.toPath(), pretty);
  }

  /**
   * Serialize an object into Json in a file
   */
  default void write(Object o, Path out, boolean pretty) {
    try {
      Files.write(out, toString(o, pretty).getBytes(UTF_8));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Serialize an object into Json in a stream
   */
  default void write(Object o, OutputStream out, boolean pretty) {
    write(o, new OutputStreamWriter(out, UTF_8), pretty);
  }

  /**
   * Serialize an object into Json in a stream
   */
  default void write(Object o, Writer out, boolean pretty) {
    try {
      out.write(toString(o, pretty));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * @author Mathieu Carbou
   */
  interface Factory {
    Json create();

    Factory pretty();

    Factory eol(String eol);

    Factory systemEOL();

    Factory pretty(boolean pretty);

    Factory withModule(Module module);

    Factory withModules(Module... modules);
  }

  interface Module {

  }
}
