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
package org.terracotta.lease;

/**
 * An abstraction over the flow of time. This allows tests to make changes to how times flows ensuring that tests are
 * not racy.
 */
public interface TimeSource {
  /**
   * Equivalent to System.nanoTime()
   * @return a long representing the current time
   */
  long nanoTime();

  /**
   * Equivalent to Thread.sleep();
   * @param milliseconds the number of milliseconds for which the current thread should sleep
   * @throws InterruptedException
   */
  void sleep(long milliseconds) throws InterruptedException;
}
