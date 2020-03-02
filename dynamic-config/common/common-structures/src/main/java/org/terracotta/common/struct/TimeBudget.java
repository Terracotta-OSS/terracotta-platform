/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.common.struct;

import java.util.concurrent.TimeUnit;

public class TimeBudget {
  private final long budgetExpiry;
  private final TimeUnit timeUnit;

  public TimeBudget(long timeout, TimeUnit timeUnit) {
    this.timeUnit = timeUnit;
    this.budgetExpiry = System.nanoTime() + TimeUnit.NANOSECONDS.convert(timeout, timeUnit);
  }

  public long remaining() {
    return this.remaining(this.timeUnit);
  }

  public long remaining(TimeUnit timeUnit) {
    long now = System.nanoTime();
    long remaining = this.budgetExpiry - now;
    return timeUnit.convert(remaining, TimeUnit.NANOSECONDS);
  }

  public String toString() {
    return "TimeBudget{budgetExpiry=" + this.budgetExpiry + ", remaining=" + this.remaining() + ", timeUnit=" + this.timeUnit + '}';
  }
}
