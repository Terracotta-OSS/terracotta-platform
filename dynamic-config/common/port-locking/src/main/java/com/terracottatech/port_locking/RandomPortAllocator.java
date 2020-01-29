/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.port_locking;

import java.util.Random;

public class RandomPortAllocator implements PortAllocator {
  private static final int LOWEST_PORT_INCLUSIVE = 1024;
  private static final int HIGHEST_PORT_INCLUSIVE = 32767;

  private final Random random;

  public RandomPortAllocator(Random random) {
    this.random = random;
  }

  @Override
  public int allocatePorts(int portCount) {
    int highestPortAdjustment = portCount - 1;
    return getRandomIntInRange(LOWEST_PORT_INCLUSIVE, HIGHEST_PORT_INCLUSIVE - highestPortAdjustment);
  }

  private int getRandomIntInRange(int min, int max) {
    return random.nextInt((max - min) + 1) + min;
  }
}
