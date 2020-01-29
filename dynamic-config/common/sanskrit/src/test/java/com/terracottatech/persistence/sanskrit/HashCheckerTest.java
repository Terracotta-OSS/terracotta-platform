/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.persistence.sanskrit;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class HashCheckerTest {
  @Test(expected = SanskritException.class)
  public void oneHashNoRecord() throws Exception {
    HashChecker checker = new HashChecker("f", null);
    checker.done();
  }

  @Test
  public void oneHashFound() throws Exception {
    HashChecker checker = new HashChecker("f", null);
    assertTrue(checker.check("a"));
    assertTrue(checker.check("b"));
    assertTrue(checker.check("c"));
    assertTrue(checker.check("f"));
    assertNull(checker.done());
    assertEquals("hash1", checker.nextHashFile());
  }

  @Test
  public void otherHashFound() throws Exception {
    HashChecker checker = new HashChecker(null, "f");
    assertTrue(checker.check("a"));
    assertTrue(checker.check("b"));
    assertTrue(checker.check("c"));
    assertTrue(checker.check("f"));
    assertNull(checker.done());
    assertEquals("hash0", checker.nextHashFile());
  }

  @Test
  public void hashFoundAfterOneHashFound() throws Exception {
    HashChecker checker = new HashChecker("e", null);
    assertTrue(checker.check("a"));
    assertTrue(checker.check("b"));
    assertTrue(checker.check("c"));
    assertTrue(checker.check("e"));
    assertFalse(checker.check("f"));
    assertNull(checker.done());
    assertEquals("hash1", checker.nextHashFile());
  }

  @Test
  public void bothHashesFound() throws Exception {
    HashChecker checker = new HashChecker("e", "f");
    assertTrue(checker.check("a"));
    assertTrue(checker.check("b"));
    assertTrue(checker.check("e"));
    assertTrue(checker.check("f"));
    assertEquals("hash0", checker.done());
    assertEquals("hash0", checker.nextHashFile());
  }

  @Test
  public void bothHashesFoundReversed() throws Exception {
    HashChecker checker = new HashChecker("f", "e");
    assertTrue(checker.check("a"));
    assertTrue(checker.check("b"));
    assertTrue(checker.check("e"));
    assertTrue(checker.check("f"));
    assertEquals("hash1", checker.done());
    assertEquals("hash1", checker.nextHashFile());
  }

  @Test
  public void hashFoundAfterBothHashesFound() throws Exception {
    HashChecker checker = new HashChecker("e", "f");
    assertTrue(checker.check("a"));
    assertTrue(checker.check("b"));
    assertTrue(checker.check("e"));
    assertTrue(checker.check("f"));
    assertFalse(checker.check("c"));
    assertEquals("hash0", checker.done());
    assertEquals("hash0", checker.nextHashFile());
  }

  @Test(expected = SanskritException.class)
  public void moreThanOneHashFoundAfterBothHashesFound() throws Exception {
    HashChecker checker = new HashChecker("e", "f");
    assertTrue(checker.check("a"));
    assertTrue(checker.check("b"));
    assertTrue(checker.check("e"));
    assertTrue(checker.check("f"));
    assertFalse(checker.check("c"));
    checker.check("d");
  }

  @Test(expected = SanskritException.class)
  public void hashBetweenHashes() throws Exception {
    HashChecker checker = new HashChecker("d", "f");
    assertTrue(checker.check("a"));
    assertTrue(checker.check("b"));
    assertTrue(checker.check("d"));
    checker.check("e");
  }

  @Test(expected = SanskritException.class)
  public void duplicateHashes() throws Exception {
    HashChecker checker = new HashChecker("e", "f");
    assertTrue(checker.check("a"));
    assertTrue(checker.check("b"));
    assertTrue(checker.check("e"));
    checker.check("e");
  }
}
