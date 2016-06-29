/**
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
package org.terracotta.management.service;

/**
 * Constants used for management service testing.
 *
 * @author RKAV
 */
public final class TestConstants {
  public static final int BUFFER_SIZE = 1 << 13;
  public static final int NUM_PARTITIONS_FOR_POOLED = 8;
  public static final int DEFAULT_MESSAGE_SIZE = 128;
  public static final int TEST_MAX_WAIT_TIME_MILLIS = 300000;
  public static void PAUSE(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException ignored) {
    }
  }
}
