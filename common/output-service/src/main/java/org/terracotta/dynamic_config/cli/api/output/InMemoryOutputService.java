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

import java.util.List;
import java.util.Queue;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;

public class InMemoryOutputService implements OutputService {

  private final Queue<String> lines = new ConcurrentLinkedQueue<>();

  public InMemoryOutputService() {
    this(emptyList());
  }

  public InMemoryOutputService(List<String> buffer) {
    this.lines.addAll(buffer);
  }

  public void clear() {
    lines.clear();
  }

  public Stream<String> lines() {
    return lines.stream();
  }

  @Override
  public String toString() {
    return "memory";
  }

  @Override
  public void out(String format, Object... args) {
    // Split using both Windows and UNIX line separators
    StringTokenizer st = new StringTokenizer(MessageFormatter.arrayFormat(format, args).getMessage(), "\n\r");
    while (st.hasMoreTokens()) {
      lines.offer(st.nextToken());
    }
  }
}
