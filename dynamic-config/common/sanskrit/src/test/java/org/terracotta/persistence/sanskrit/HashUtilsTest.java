/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.persistence.sanskrit;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HashUtilsTest {
  @Test
  public void emptyString() {
    assertEquals("990e2617bc776c82671a73a8ca1890e2ff25dc48", HashUtils.generateHash(""));
  }

  @Test
  public void concatenation() {
    assertEquals("e65287c61b6fa7e061f62f9fe1979a1dde62e447", HashUtils.generateHash("abc"));
    assertEquals("e65287c61b6fa7e061f62f9fe1979a1dde62e447", HashUtils.generateHash("a", "b", "c"));
  }

  @Test
  public void multiByteCharacter() {
    assertEquals("f72730c87ac92d97c723605ac7bc33fe16e92255", HashUtils.generateHash("Ɵ"));
  }
}
