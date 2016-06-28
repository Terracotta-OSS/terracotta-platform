/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Entity Management Service.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
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
