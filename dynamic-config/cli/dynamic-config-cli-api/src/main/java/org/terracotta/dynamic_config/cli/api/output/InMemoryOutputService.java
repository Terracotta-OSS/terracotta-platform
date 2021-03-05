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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

public class InMemoryOutputService implements OutputService {

  private final List<String> lines;

  public InMemoryOutputService() {
    this(new LinkedList<>());
  }

  public InMemoryOutputService(List<String> buffer) {
    this.lines = buffer;
  }

  public synchronized void clear() {
    lines.clear();
  }

  public synchronized List<String> getOutput() {
    return new ArrayList<>(lines);
  }

  @Override
  public synchronized String toString() {
    return "memory";
  }

  @Override
  public synchronized void out(String format, Object... args) {
    // Split using both Windows and UNIX line separators
    StringTokenizer st = new StringTokenizer(MessageFormatter.arrayFormat(format, args).getMessage(), "\n\r");
    while (st.hasMoreTokens()) {
      lines.add(st.nextToken());
    }
  }
}
