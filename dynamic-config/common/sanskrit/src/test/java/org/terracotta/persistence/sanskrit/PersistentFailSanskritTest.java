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
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PersistentFailSanskritTest {
  @Mock
  private Sanskrit underlying;

  @Test
  public void readFromUnderlying() throws Exception {
    when(underlying.getString("A")).thenReturn("a");

    PersistentFailSanskrit sanskrit = new PersistentFailSanskrit(underlying);
    assertEquals("a", sanskrit.getString("A"));
  }

  @Test(expected = SanskritException.class)
  public void failOnceFailAgain() throws Exception {
    when(underlying.getString("A")).thenThrow(SanskritException.class).thenReturn("a");

    PersistentFailSanskrit sanskrit = new PersistentFailSanskrit(underlying);
    try {
      sanskrit.getString("A");
      fail("Expected exception");
    } catch (SanskritException e) {
      // Expected
    }

    sanskrit.getString("A");
  }
}
