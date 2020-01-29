/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
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
