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
package org.terracotta.nomad.client.results;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.lang.System.lineSeparator;

/**
 * @author Mathieu Carbou
 */
public class NomadFailureReceiver<T> extends LoggingResultReceiver<T> {

  private final List<String> failures = new CopyOnWriteArrayList<>();

  @Override
  protected void error(String line) {
    failures.add(line);
  }

  public List<String> getFailures() {
    return failures;
  }

  public boolean isEmpty() {
    return failures.isEmpty();
  }

  public void reThrow() throws IllegalStateException {
    if (!isEmpty()) {
      StringBuilder msg = new StringBuilder("Two-Phase commit failed with " + failures.size() + " messages(s):" + lineSeparator() + lineSeparator());
      for (int i = 0; i < failures.size(); i++) {
        if (msg.charAt(msg.length() - 1) != '\n') {
          msg.append(lineSeparator());
        }
        msg.append("(").append(i + 1).append(") ").append(failures.get(i));
      }
      throw new IllegalStateException(msg.toString());
    }
  }
}
