/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.diagnostic.client.connection;

public class ConcurrencySizing {

  private static final int MAX_THREAD_COUNT = 64;

  private final int max;

  public ConcurrencySizing() {
    this(MAX_THREAD_COUNT);
  }

  public ConcurrencySizing(int max) {
    this.max = max;
  }

  public int getThreadCount(int serverCount) {
    return Math.min(serverCount, max);
  }
}
