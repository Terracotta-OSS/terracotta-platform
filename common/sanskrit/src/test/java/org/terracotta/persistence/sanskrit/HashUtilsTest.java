/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
