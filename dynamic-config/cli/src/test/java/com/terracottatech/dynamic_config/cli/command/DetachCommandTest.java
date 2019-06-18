/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.command;

import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * @author Mathieu Carbou
 */
public class DetachCommandTest extends TopologyCommandTest<DetachCommand> {
  @Override
  protected DetachCommand newCommand() {
    return new DetachCommand(nodeAddressDiscovery, connectionFactory);
  }

  @Test
  public void test_detach_node_from_stripe() {
    fail("TODO");
  }

  @Test
  public void test_detach_stripe() {
    fail("TODO");
  }
}