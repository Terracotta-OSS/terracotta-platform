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

  private final List<String> reasons = new CopyOnWriteArrayList<>();
  private final List<Throwable> errors = new CopyOnWriteArrayList<>();

  @Override
  protected void error(String line, Throwable e) {
    reasons.add(e == null ? line : (line + ". Reason: " + stringify(e)));
    if (e != null) {
      errors.add(e);
    }
  }

  public List<String> getReasons() {
    return reasons;
  }

  public boolean isEmpty() {
    return reasons.isEmpty();
  }

  public void reThrowReasons() throws IllegalStateException {
    if (!isEmpty()) {
      throw buildError();
    }
  }

  public void reThrowErrors() throws IllegalStateException {
    if (!isEmpty()) {
      IllegalStateException error = buildError();
      errors.forEach(error::addSuppressed);
      throw error;
    }
  }

  private IllegalStateException buildError() {
    StringBuilder msg = new StringBuilder("Two-Phase commit failed with " + reasons.size() + " messages(s):" + lineSeparator() + lineSeparator());
    for (int i = 0; i < reasons.size(); i++) {
      if (msg.charAt(msg.length() - 1) != '\n') {
        msg.append(lineSeparator());
      }
      msg.append("(").append(i + 1).append(") ").append(reasons.get(i));
    }
    return new IllegalStateException(msg.toString());
  }
}
