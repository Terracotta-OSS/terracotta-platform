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
package org.terracotta.offheapresource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.function.Consumer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class OffHeapResourceTest {
  @Mock
  private Consumer<OffHeapUsageEvent> onThresholdChange;

  @Mock
  private CapacityChangeHandler onCapacityChange;

  private String identifier = "id";

  @Test
  public void testNegativeResourceSize() {
    try {
      new OffHeapResourceImpl(identifier, -1);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      //expected;
    }
  }

  @Test
  public void testZeroSizeResourceIsUseless() {
    OffHeapResource ohr = new OffHeapResourceImpl(identifier, 0);
    assertThat(ohr.reserve(1), is(false));
    assertThat(ohr.available(), is(0L));
  }

  @Test
  public void testAllocationReducesSize() {
    OffHeapResource ohr = new OffHeapResourceImpl(identifier, 20);
    assertThat(ohr.capacity(), is(20L));
    assertThat(ohr.available(), is(20L));
    assertThat(ohr.reserve(10), is(true));
    assertThat(ohr.available(), is(10L));
    assertThat(ohr.capacity(), is(20L));
  }

  @Test
  public void testNegativeAllocationFails() {
    OffHeapResource ohr = new OffHeapResourceImpl(identifier, 20);
    try {
      ohr.reserve(-1);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      //expected
    }
  }

  @Test
  public void testAllocationWhenExhaustedFails() {
    OffHeapResource ohr = new OffHeapResourceImpl(identifier, 20);
    ohr.reserve(20);
    assertThat(ohr.reserve(1), is(false));
    assertThat(ohr.available(), is(0L));
  }

  @Test
  public void testFreeIncreasesSize() {
    OffHeapResource ohr = new OffHeapResourceImpl(identifier, 20);
    ohr.reserve(20);
    assertThat(ohr.available(), is(0L));
    ohr.release(10);
    assertThat(ohr.available(), is(10L));
  }

  @Test
  public void testNegativeFreeFails() {
    OffHeapResource ohr = new OffHeapResourceImpl(identifier, 20);
    ohr.reserve(10);
    try {
      ohr.release(-10);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      //expected
    }
  }

  @Test
  public void testThresholds() {
    OffHeapResourceImpl offHeapResource = new OffHeapResourceImpl(identifier, 10);
    // TODO move to manipulating the logger to be able to do real assertions.
    offHeapResource.reserve(4); // Does not print a log statement
    offHeapResource.reserve(4); // Does print an info log statement
    offHeapResource.reserve(1); // Does print a warn log statement
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSetCapacityNegative() {
    OffHeapResourceImpl ohr = new OffHeapResourceImpl(identifier, 20L, onThresholdChange, onCapacityChange);
    ohr.setCapacity(-1L);
    verifyNoMoreInteractions(onCapacityChange);
  }

  @Test
  public void testSetCapacityBigger() {
    OffHeapResourceImpl ohr = new OffHeapResourceImpl(identifier, 20L, onThresholdChange, onCapacityChange);
    ohr.reserve(14L);
    assertThat(ohr.setCapacity(30L), is(true));
    assertThat(ohr.capacity(), is(30L));
    assertThat(ohr.available(), is(16L));
    verify(onCapacityChange).onCapacityChanged(ohr, 20L, 30L);
  }

  @Test
  public void testSetCapacitySmaller() {
    OffHeapResourceImpl ohr = new OffHeapResourceImpl(identifier, 20L, onThresholdChange, onCapacityChange);
    ohr.reserve(14L);
    assertThat(ohr.setCapacity(16L), is(true));
    assertThat(ohr.capacity(), is(16L));
    assertThat(ohr.available(), is(2L));
    verify(onCapacityChange).onCapacityChanged(ohr, 20L, 16L);
  }

  @Test
  public void testSetCapacityToReserved() {
    OffHeapResourceImpl ohr = new OffHeapResourceImpl(identifier, 20L, onThresholdChange, onCapacityChange);
    ohr.reserve(14L);
    assertThat(ohr.setCapacity(14L), is(true));
    assertThat(ohr.capacity(), is(14L));
    assertThat(ohr.available(), is(0L));
    verify(onCapacityChange).onCapacityChanged(ohr, 20L, 14L);
  }

  @Test
  public void testSetCapacityTooSmall() {
    OffHeapResourceImpl ohr = new OffHeapResourceImpl(identifier, 20L, onThresholdChange, onCapacityChange);
    ohr.reserve(14L);
    assertThat(ohr.setCapacity(13L), is(false));
    assertThat(ohr.capacity(), is(20L));
    assertThat(ohr.available(), is(6L));
    verifyNoMoreInteractions(onCapacityChange);
  }
}
