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
package org.terracotta.persistence.sanskrit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class HashUtils {
  private static final Logger LOGGER = LoggerFactory.getLogger(HashUtils.class);

  private static final byte[] PRIVATE_BYTES = toBytes(
      0x71, 0x0d, 0x71, 0xd1, 0xca, 0xcd, 0xd1, 0xe1,
      0x8b, 0x56, 0x87, 0x66, 0x0e, 0x35, 0x84, 0xeb,
      0xbd, 0xf4, 0x8c, 0xa1, 0x01, 0x41, 0xf3, 0x9f,
      0xf6, 0x64, 0x7e, 0x33, 0x8e, 0xed, 0xd1, 0x5b,
      0xd1, 0x75, 0x3e, 0x67, 0xeb, 0x76, 0x1c, 0x50,
      0x94, 0x3d, 0xbe, 0x56, 0xc8, 0x42, 0xe0, 0x9e,
      0x28, 0xb5, 0xca, 0xd5, 0x49, 0xd9, 0xcc, 0x77,
      0xba, 0x69, 0x49, 0x45, 0xd8, 0x1b, 0x49, 0xc9
  );

  private static byte[] toBytes(int... ints) {
    byte[] bytes = new byte[ints.length];

    for (int i = 0; i < ints.length; i++) {
      bytes[i] = (byte) ints[i];
    }

    return bytes;
  }

  public static String generateHash(String... inputs) {
    return generateHash(String.join("", inputs));
  }

  public static String generateHash(String input) {
    final String hash = generateHash(input.getBytes(StandardCharsets.UTF_8));
    LOGGER.trace("generateHash({}): {}", input, hash);
    return hash;
  }

  public static String generateHash(byte[] input) {
    return toHexText(Arrays.copyOf(digest(input), 20));
  }

  private static byte[] digest(byte[] input) {
    try {
      MessageDigest digester = MessageDigest.getInstance("SHA-512");
      digester.update(input);
      digester.update(PRIVATE_BYTES);
      return digester.digest();
    } catch (NoSuchAlgorithmException e) {
      throw new AssertionError("Missing hash algorithm: SHA-512");
    }
  }

  private static String toHexText(byte[] input) {
    StringBuilder sb = new StringBuilder(input.length * 2);

    for (byte b : input) {
      sb.append(String.format("%02x", b));
    }

    return sb.toString();
  }
}
