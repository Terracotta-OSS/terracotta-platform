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
package org.terracotta.dynamic_config.cli.api.output;

import org.slf4j.helpers.MessageFormatter;

import java.io.PrintStream;

/**
 * Responsible for handling the redirection of the output of config tool commands.
 */
public class OutputService {

  private final PrintStream outStream;
  private final PrintStream errStream;

  public OutputService() {
    outStream = System.out;
    errStream = System.err;
  }

  public OutputService(PrintStream outStream, PrintStream errStream) {
    this.outStream = outStream;
    this.errStream = errStream;
  }

  public void out(String format, Object... args) {
    outStream.println(MessageFormatter.arrayFormat(format, args).getMessage());
  }

  public void info(String format, Object... args) {
    errStream.println(MessageFormatter.arrayFormat(format, args).getMessage());
  }
}
