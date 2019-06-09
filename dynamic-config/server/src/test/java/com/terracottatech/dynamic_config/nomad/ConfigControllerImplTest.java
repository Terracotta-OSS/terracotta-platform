/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.nomad;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ConfigControllerImplTest {
  @Test
  public void testGetNodeName() {
    ConfigControllerImpl configController = new ConfigControllerImpl(() -> "node0", () -> "stripe1");
    assertThat(configController.getNodeName(), is("node0"));
  }

  @Test
  public void testGetStripeName() {
    ConfigControllerImpl configController = new ConfigControllerImpl(() -> "node0", () -> "stripe1");
    assertThat(configController.getStripeName(), is("stripe1"));
  }
}
