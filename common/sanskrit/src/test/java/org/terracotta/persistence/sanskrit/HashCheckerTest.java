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
package org.terracotta.persistence.sanskrit;

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
