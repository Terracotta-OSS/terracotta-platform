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
package org.terracotta.dynamic_config.api.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.TreeSet;

import static java.util.Objects.requireNonNull;

/**
 * Utility class used to write a property file without the date header and with the properties sorted
 *
 * @author Mathieu Carbou
 */
public class Props {

  public static Properties load(String content) {
    return load(new StringReader(content));
  }

  @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
  public static Properties load(Path propertiesFile) {
    requireNonNull(propertiesFile);
    if (propertiesFile.getFileName() == null || !propertiesFile.getFileName().toString().endsWith(".properties")) {
      throw new IllegalArgumentException("Expected a properties file, but got " + propertiesFile.getFileName());
    }

    Properties props = new Properties();
    try (Reader in = new InputStreamReader(Files.newInputStream(propertiesFile), StandardCharsets.UTF_8)) {
      props.load(in);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to read config file: " + propertiesFile.getFileName() + ". Make sure the file exists and is readable", e);
    }
    return props;
  }

  /**
   * Warning: caller whi created the stream is also responsible for closing it
   */
  public static Properties load(InputStream inputStream) {
    Properties props = new Properties();
    try {
      props.load(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return props;
  }

  /**
   * Warning: caller whi created the stream is also responsible for closing it
   */
  public static Properties load(Reader reader) {
    Properties props = new Properties();
    try {
      props.load(reader);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return props;
  }

  public static String toString(Properties properties) {
    return toString(properties, null);
  }

  public static String toString(Properties properties, String comment) {
    StringWriter out = new StringWriter();
    store(out, properties, comment);
    return out.toString();
  }

  public static void store(Writer out, Properties properties, String comment) {
    try {
      StringWriter tmp = new StringWriter();
      Properties copy = new Properties() {
        private static final long serialVersionUID = 1L;

        // used to sort the lines in the output
        @Override
        public synchronized Enumeration<Object> keys() {
          return Collections.enumeration(new TreeSet<>(properties.keySet()));
        }
      };
      copy.putAll(properties);
      copy.store(tmp, comment);
      String content = tmp.toString();
      final int secondLineStart = content.indexOf('\n') + 1;
      if (secondLineStart != 0) {
        if (comment == null) {
          content = content.substring(secondLineStart);
        } else {
          content = content.substring(0, secondLineStart) + content.substring(content.indexOf('\n', secondLineStart) + 1);
        }
      }
      out.write(content);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
