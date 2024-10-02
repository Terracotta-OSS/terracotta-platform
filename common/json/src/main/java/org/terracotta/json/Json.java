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
package org.terracotta.json;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Terracotta Json
 *
 * @author Mathieu Carbou
 */
public interface Json {

  // forces a serialization to null
  Null NULL = new Null() {};

  interface Null {}

  // map

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

  // parseObject

  /**
   * Parses a json content (object expected) in a file into a Map structure
   */
  default Map<String, Object> parseObject(File file) {
    return parseObject(file, UTF_8);
  }

  default Map<String, Object> parseObject(File file, Charset charset) {
    return parseObject(file.toPath(), charset);
  }

  /**
   * Parses a json content (object expected) in a file into a Map structure
   */
  default Map<String, Object> parseObject(Path path) {
    return parseObject(path, UTF_8);
  }

  default Map<String, Object> parseObject(Path path, Charset charset) {
    try (Reader r = Files.newBufferedReader(path, charset)) {
      return parseObject(r);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Parses a json content (object expected) into a Map structure
   */
  default Map<String, Object> parseObject(InputStream json) {
    return parseObject(json, UTF_8);
  }

  default Map<String, Object> parseObject(InputStream json, Charset charset) {
    return parseObject(new InputStreamReader(json, charset));
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
  default Map<String, Object> parseObject(String json) {
    return parseObject(new StringReader(json));
  }

  /**
   * Parses a json content (object expected) into a Map structure
   */
  Map<String, Object> parseObject(Reader r);

  // parseList

  /**
   * Parses a json content (list expected) in a file into a List structure
   */
  default List<Object> parseList(File file) {
    return parseList(file, UTF_8);
  }

  default List<Object> parseList(File file, Charset charset) {
    return parseList(file.toPath(), charset);
  }

  default List<Object> parseList(Path path) {
    return parseList(path, UTF_8);
  }

  default List<Object> parseList(Path path, Charset charset) {
    try (Reader r = Files.newBufferedReader(path, charset)) {
      return parseList(r);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Parses a json content (list expected) into a List structure
   */
  default List<Object> parseList(InputStream json) {
    return parseList(json, UTF_8);
  }

  default List<Object> parseList(InputStream json, Charset charset) {
    return parseList(new InputStreamReader(json, charset));
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
  default List<Object> parseList(String json) {
    return parseList(new StringReader(json));
  }

  /**
   * Parses a json content (list expected) into a List structure
   */
  List<Object> parseList(Reader r);

  // parse

  /**
   * Parses a json content in a file into an unknown structure (Map, List, Number, String)
   */
  default Object parse(File file) {
    return parse(file.toPath(), UTF_8, Object.class);
  }

  default Object parse(File file, Charset charset) {
    return parse(file.toPath(), charset, Object.class);
  }

  /**
   * Parses a json content in a file into a Java model
   */
  default <T> T parse(File file, Class<T> type) {
    return parse(file.toPath(), UTF_8, type);
  }

  default Object parse(File file, Type type) {
    return parse(file.toPath(), UTF_8, type);
  }

  default <T> T parse(File file, Charset charset, Class<T> type) {
    return parse(file.toPath(), charset, type);
  }

  default Object parse(File file, Charset charset, Type type) {
    return parse(file.toPath(), charset, type);
  }

  /**
   * Parses a json content in a file into an unknown structure (Map, List, Number, String)
   */
  default Object parse(Path path) {
    return parse(path, UTF_8, Object.class);
  }

  default Object parse(Path path, Charset charset) {
    return parse(path, charset, Object.class);
  }

  /**
   * Parses a json content in a file into a Java model
   */
  default <T> T parse(Path path, Class<T> type) {
    return parse(path, UTF_8, type);
  }

  default Object parse(Path path, Type type) {
    return parse(path, UTF_8, type);
  }

  default <T> T parse(Path path, Charset charset, Class<T> type) {
    try (Reader r = Files.newBufferedReader(path, charset)) {
      return parse(r, type);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  default Object parse(Path path, Charset charset, Type type) {
    try (Reader r = Files.newBufferedReader(path, charset)) {
      return parse(r, type);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Parses a json content into an unknown structure (Map, List, Number, String)
   */
  default Object parse(InputStream json) {
    return parse(json, UTF_8, Object.class);
  }

  default Object parse(InputStream json, Charset charset) {
    return parse(json, charset, Object.class);
  }

  /**
   * Parses a json content from a stream into a Java model
   */
  default <T> T parse(InputStream is, Class<T> type) {
    return parse(is, UTF_8, type);
  }

  default Object parse(InputStream is, Type type) {
    return parse(is, UTF_8, type);
  }

  default <T> T parse(InputStream is, Charset charset, Class<T> type) {
    return parse(new InputStreamReader(is, charset), type);
  }

  default Object parse(InputStream is, Charset charset, Type type) {
    return parse(new InputStreamReader(is, charset), type);
  }

  /**
   * Parses a json content into an unknown structure (Map, List, Number, String)
   */
  default Object parse(URL url) {
    return parse(url, Object.class);
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
   * Parses a json content into an unknown structure (Map, List, Number, String)
   */
  default Object parse(String json) {
    return parse(json, Object.class);
  }

  /**
   * Parses a json content into a Java model
   */
  default <T> T parse(String json, Class<T> type) {
    return parse(new StringReader(json), type);
  }

  default Object parse(String json, Type type) {
    return parse(new StringReader(json), type);
  }

  /**
   * Parses a json content into an unknown structure (Map, List, Number, String)
   */
  default Object parse(Reader r) {
    return parse(r, Object.class);
  }

  /**
   * Parses a json content from a stream into a Java model
   */
  <T> T parse(Reader r, Class<T> type);

  Object parse(Reader r, Type type);

  // toString

  /**
   * Serialize an object into a Json string
   */
  String toString(Object o);

  // write

  default void write(Object o, File out) {
    write(o, out.toPath(), UTF_8);
  }

  default void write(Object o, File out, Charset charset) {
    write(o, out.toPath(), charset);
  }

  default void write(Object o, Path out) {
    write(o, out, UTF_8);
  }

  /**
   * Serialize an object into Json in a file
   */
  default void write(Object o, Path out, Charset charset) {
    try {
      Files.write(out, toString(o).getBytes(charset));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  default void write(Object o, OutputStream out) {
    write(o, out, UTF_8);
  }

  /**
   * Serialize an object into Json in a stream
   */
  default void write(Object o, OutputStream out, Charset charset) {
    write(o, new OutputStreamWriter(out, charset));
  }

  /**
   * Serialize an object into Json in a stream
   */
  default void write(Object o, Writer out) {
    try {
      out.write(toString(o));
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

    Factory pretty(boolean pretty);

    Factory withClassLoader(ClassLoader classLoader);

    Factory withModule(Module module);

    Factory withModules(Collection<Module> modules);
  }

  interface Module {
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Inherited
    @interface Dependencies {
      Class<? extends Module>[] value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Inherited
    @interface Overrides {
      Class<? extends Module>[] value();
    }
  }
}
