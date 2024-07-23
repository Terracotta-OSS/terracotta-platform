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
package org.terracotta.dynamic_config.cli.api.output;

import java.io.Closeable;

/**
 * Responsible for handling the redirection of the output of config tool commands.
 */
public interface OutputService extends Closeable {
  void out(String format, Object... args);

  default void info(String format, Object... args) {
    out(format, args);
  }

  default void warn(String format, Object... args) {
    out(format, args);
  }

  @Override
  default void close() {}

  default OutputService then(OutputService after) {
    OutputService first = this;
    return new OutputService() {
      @Override
      public void out(String format, Object... args) {
        first.out(format, args);
        after.out(format, args);
      }

      @Override
      public void info(String format, Object... args) {
        first.info(format, args);
        after.info(format, args);
      }

      @Override
      public void warn(String format, Object... args) {
        first.warn(format, args);
        after.warn(format, args);
      }

      @Override
      public void close() {
        first.close();
        after.close();
      }

      @Override
      public String toString() {
        return first + " then " + after;
      }
    };
  }
}
