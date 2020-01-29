/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.diagnostic.client.connection;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ConcurrencySizingTest {
  @Test
  public void small() {
    assertEquals(10, new ConcurrencySizing().getThreadCount(10));
  }

  @Test
  public void large() {
    assertEquals(64, new ConcurrencySizing().getThreadCount(100));
  }
}
