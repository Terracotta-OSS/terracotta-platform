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
package org.terracotta.diagnostic.client.connection;

public class ConcurrencySizing {

  private static final int MAX_THREAD_COUNT = 64;

  private final int max;

  public ConcurrencySizing() {
    this(MAX_THREAD_COUNT);
  }

  public ConcurrencySizing(int max) {
    this.max = max;
  }

  public int getThreadCount(int serverCount) {
    return Math.min(serverCount, max);
  }
}
