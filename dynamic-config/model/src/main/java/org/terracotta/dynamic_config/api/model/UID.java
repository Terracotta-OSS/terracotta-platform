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
package org.terracotta.dynamic_config.api.model;

import java.math.BigInteger;
import java.util.Base64;
import java.util.Random;
import java.util.UUID;

/**
 * @author Mathieu Carbou
 */
public class UID {

  /**
   * Generate a Java-like UUID in a shorter string B64 encoded
   */
  public static String newUID() {
    UUID uuid = UUID.randomUUID();
    return encodeB64(toBytes(new long[]{uuid.getMostSignificantBits(), uuid.getLeastSignificantBits()}));
  }

  /**
   * Generate a UID in a shorter string B64 encoded.
   * <p>
   * This method takes a seed to be able to control the UID generator sequence.
   */
  public static String newUID(Random r) {
    byte[] bytes = new byte[16];
    r.nextBytes(bytes);
    // similar to JDK's UUID impl
    bytes[6] &= 0x0f;  /* clear version        */
    bytes[6] |= 0x40;  /* set to version 4     */
    bytes[8] &= 0x3f;  /* clear variant        */
    bytes[8] |= 0x80;  /* set to IETF variant  */
    return encodeB64(bytes);
  }

  public static boolean isUID(String s) {
    try {
      final byte[] decoded = decodeB64(s);
      return decoded.length == 16;
    } catch (Exception e) {
      return false;
    }
  }

  // please keep for tests / verification purposes.
  // longs[] longs = new long[]{uuid.getMostSignificantBits(), uuid.getLeastSignificantBits()};
  // assert Arrays.equals(longs, toLongs(toBytes(longs)))
  static long[] toLongs(byte[] buffer) {
    long msb = 0;
    long lsb = 0;
    for (int i = 0; i < 8; i++) {
      msb = (msb << 8) | (buffer[i] & 0xff);
    }
    for (int i = 8; i < 16; i++) {
      lsb = (lsb << 8) | (buffer[i] & 0xff);
    }
    return new long[]{msb, lsb};
  }

  static String toHex(byte[] b) {
    return new BigInteger(1, b).toString(16);
  }

  static byte[] toBytes(long[] longs) {
    long msb = longs[0];
    long lsb = longs[1];
    byte[] bytes = new byte[16];
    for (int i = 7; i >= 0; i--) {
      bytes[i] = (byte) (msb & 0xff);
      msb >>>= 8;
    }
    for (int i = 15; i >= 8; i--) {
      bytes[i] = (byte) (lsb & 0xff);
      lsb >>>= 8;
    }
    return bytes;
  }

  static String encodeB64(byte[] data) {
    return Base64.getEncoder().encodeToString(data)
        // java-8 and other - compatible B64 url decoder use - and _ instead of + and /
        // padding can be ignored to shorten the UUID
        .replace('+', '-')
        .replace('/', '_')
        .replace("=", "");
  }

  static byte[] decodeB64(String s) {
    return Base64.getDecoder().decode(s
        // java-8 and other - compatible B64 url decoder use - and _ instead of + and /
        // padding can be ignored to shorten the UUID
        .replace('-', '+')
        .replace('_', '/'));
  }
}
