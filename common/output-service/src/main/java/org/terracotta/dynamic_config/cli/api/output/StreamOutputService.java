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

import org.slf4j.helpers.MessageFormatter;

import java.io.PrintStream;

public class StreamOutputService implements OutputService {

  protected final PrintStream out;
  protected final PrintStream err;

  public StreamOutputService(PrintStream out, PrintStream err) {
    this.out = out;
    this.err = err;
  }

  @Override
  public void out(String format, Object... args) {
    out.println(MessageFormatter.arrayFormat(format, args).getMessage());
  }

  @Override
  public void info(String format, Object... args) {
    err.println(MessageFormatter.arrayFormat(format, args).getMessage());
  }

  @Override
  public void warn(String format, Object... args) {
    err.println(MessageFormatter.arrayFormat(format, args).getMessage());
  }

  @Override
  public void close() {
    out.close();
    err.close();
  }

  @Override
  public String toString() {
    return "streaming";
  }
}
