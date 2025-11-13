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
package org.terracotta.security.logger;

/**
 * @author Mathieu Carbou
 */
public interface SecurityLogger {

  /**
   * Logs a security-related event
   *
   * @param message the message to log
   * @param args the arguments to use when formatting the message using parameterized formatting
   * @see <a href="https://www.slf4j.org/faq.html#logging_performance">https://www.slf4j.org/faq.html#logging_performance</a>
   */
  void log(String message, Object... args);

  SecurityLogger NOOP = new SecurityLogger() {
    @Override
    public void log(String message, Object... args) {
      // no-op
    }
  };
}
