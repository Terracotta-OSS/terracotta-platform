/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2026
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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LeaseReconnectDataTest {

  // golden wire frame, captured from the pre-migration codec
  private static final String GOLDEN_RECONNECT_DATA = "0a0a080000000000000063";

  private static String hex(byte[] bytes) {
    StringBuilder sb = new StringBuilder();
    for (byte b : bytes) {
      sb.append(Character.forDigit(b >> 4 & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
    }
    return sb.toString();
  }

  private static byte[] fromHex(String hex) {
    byte[] bytes = new byte[hex.length() / 2];
    for (int i = 0; i < bytes.length; i++) {
      bytes[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
    }
    return bytes;
  }

  @Test
  public void reconnectDataGoldenBytes() throws Exception {
    assertEquals(GOLDEN_RECONNECT_DATA, hex(new LeaseReconnectData(99L).encode()));
    assertEquals(99L, LeaseReconnectData.decode(fromHex(GOLDEN_RECONNECT_DATA)).getConnectionSequenceNumber());
    assertEquals(GOLDEN_RECONNECT_DATA, hex(LeaseReconnectData.decode(fromHex(GOLDEN_RECONNECT_DATA)).encode()));
  }

  @Test
  public void roundtripLeaseReconnectData() throws Exception {
    LeaseReconnectData reconnectData = new LeaseReconnectData(7);
    byte[] bytes = reconnectData.encode();
    LeaseReconnectData roundtrippedData = LeaseReconnectData.decode(bytes);
    assertEquals(7, roundtrippedData.getConnectionSequenceNumber());
  }
}
