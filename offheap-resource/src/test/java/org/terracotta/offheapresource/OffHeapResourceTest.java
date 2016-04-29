/*
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is OffHeap Resource.
 *
 * The Initial Developer of the Covered Software is
 * Terracotta, Inc., a Software AG company
 */

package org.terracotta.offheapresource;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import org.junit.Test;

public class OffHeapResourceTest {

  @Test
  public void testNegativeResourceSize() {
    try {
      new OffHeapResourceImpl(-1);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      //expected;
    }
  }

  @Test
  public void testZeroSizeResourceIsUseless() {
    OffHeapResource ohr = new OffHeapResourceImpl(0);
    assertThat(ohr.reserve(1), is(false));
    assertThat(ohr.available(), is(0L));
  }

  @Test
  public void testAllocationReducesSize() {
    OffHeapResource ohr = new OffHeapResourceImpl(20);
    assertThat(ohr.available(), is(20L));
    assertThat(ohr.reserve(10), is(true));
    assertThat(ohr.available(), is(10L));
  }

  @Test
  public void testNegativeAllocationFails() {
    OffHeapResource ohr = new OffHeapResourceImpl(20);
    try {
      ohr.reserve(-1);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      //expected
    }
  }

  @Test
  public void testAllocationWhenExhaustedFails() {
    OffHeapResource ohr = new OffHeapResourceImpl(20);
    ohr.reserve(20);
    assertThat(ohr.reserve(1), is(false));
    assertThat(ohr.available(), is(0L));
  }

  @Test
  public void testFreeIncreasesSize() {
    OffHeapResource ohr = new OffHeapResourceImpl(20);
    ohr.reserve(20);
    assertThat(ohr.available(), is(0L));
    ohr.release(10);
    assertThat(ohr.available(), is(10L));
  }

  @Test
  public void testNegativeFreeFails() {
    OffHeapResource ohr = new OffHeapResourceImpl(20);
    ohr.reserve(10);
    try {
      ohr.release(-10);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      //expected
    }
  }
}
