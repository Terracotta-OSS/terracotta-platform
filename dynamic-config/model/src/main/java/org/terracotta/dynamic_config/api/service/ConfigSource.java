/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.terracotta.dynamic_config.api.model.ConfigFormat;

public interface ConfigSource {
  ConfigFormat getConfigFormat();

  Properties getConfigProperties();

  /**
   * Creates a {@code ConfigSource} from a given input, which could be a path or - for stdin.
   *
   * Note: if passed from STDIN, the config must be in the new "config" format and in UTF-8.
   */
  static ConfigSource from(String input) {
    if (input.equals("-")) {
      return from(new InputStreamReader(System.in, StandardCharsets.UTF_8));
    } else {
      return from(Paths.get(input));
    }
  }

  /**
   * Creates a {@code ConfigSource} from a file ending with ".properties" or ".config".
   */
  static ConfigSource from(Path path) {
    ConfigFormat format = ConfigFormat.from(path);
    return new ConfigSource() {
      @Override
      public ConfigFormat getConfigFormat() {
        return format;
      }

      @Override
      public Properties getConfigProperties() {
        switch (format) {
          case PROPERTIES:
            return Props.load(path);
          case CONFIG:
            return new ConfigPropertiesTranslator().load(path);
          default:
            throw new IllegalArgumentException("Invalid format: " + format + ". Supported formats: " + String.join(", ", ConfigFormat.supported()));
        }
      }

      @Override
      public String toString() {
        return path.toString();
      }
    };
  }

  /**
   * Creates a {@code ConfigSource} from a character stream.
   *
   * The config must be in the new "config" format.
   *
   * Note: caller is responsible for closing the stream.
   */
  static ConfigSource from(Reader reader) {
    return new ConfigSource() {
      @Override
      public ConfigFormat getConfigFormat() {
        return ConfigFormat.CONFIG;
      }

      @Override
      public Properties getConfigProperties() {
        try {
          return new ConfigPropertiesTranslator().convert(reader);
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      }

      @Override
      public String toString() {
        return "-";
      }
    };
  }
}
