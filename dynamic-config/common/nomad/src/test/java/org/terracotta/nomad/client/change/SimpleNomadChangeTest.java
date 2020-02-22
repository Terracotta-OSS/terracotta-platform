/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.nomad.client.change;

import org.junit.Test;
import org.terracotta.nomad.SimpleNomadChange;

import static org.junit.Assert.assertEquals;

public class SimpleNomadChangeTest {
  @Test
  public void gettersWork() {
    SimpleNomadChange changeData = new SimpleNomadChange("a", "b");
    assertEquals("a", changeData.getChange());
    assertEquals("b", changeData.getSummary());
  }
}
