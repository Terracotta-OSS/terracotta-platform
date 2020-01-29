/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.port_locking;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.security.SecureRandom;
import java.util.Random;

@RunWith(MockitoJUnitRunner.class)
public class RandomPortAllocatorTest {
  @Test
  public void withinRange() {
    int seed = new SecureRandom().nextInt();
    PortAllocator allocator = new RandomPortAllocator(new Random(seed));

    for (int i = 0; i < 10_000; i++) {
      int portBase = allocator.allocatePorts(4);
      if (portBase < 1024 || portBase > 32764) {
        throw new AssertionError("portBase outside range: " + portBase + " seed: " + seed);
      }
    }
  }
}
