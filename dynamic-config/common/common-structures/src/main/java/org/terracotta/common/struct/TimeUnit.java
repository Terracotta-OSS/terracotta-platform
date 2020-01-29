/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.common.struct;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import static java.lang.Math.multiplyExact;

public enum TimeUnit implements Unit<TimeUnit> {
  MILLISECONDS("ms", 1),
  SECONDS("s", 1000),
  MINUTES("m", 60 * 1000),
  HOURS("h", 60 * 60 * 1000),
  ;

  private final String shortName;
  private final long factor;

  TimeUnit(String shortName, long factor) {
    this.shortName = shortName;
    this.factor = factor;
  }

  @Override
  public String getShortName() {
    return shortName;
  }

  @Override
  public long convert(long quantity, TimeUnit unit) {
    if (this == unit) {
      return quantity;
    }
    return multiplyExact(quantity, unit.factor) / this.factor;
  }

  @Override
  public TimeUnit getBaseUnit() {
    return SECONDS;
  }

  @Override
  public String toString() {
    return getShortName();
  }

  public long toMillis(long quantity) {
    return MILLISECONDS.convert(quantity, this);
  }

  public long toSeconds(long quantity) {
    return SECONDS.convert(quantity, this);
  }

  public long toMinutes(long quantity) {
    return MINUTES.convert(quantity, this);
  }

  public long toHours(long quantity) {
    return HOURS.convert(quantity, this);
  }

  public static Optional<TimeUnit> from(String shortName) {
    return Arrays.stream(TimeUnit.values()).filter(timeUnit -> timeUnit.shortName.equals(shortName)).findFirst();
  }

  public static Optional<TimeUnit> from(java.util.concurrent.TimeUnit jdkTimeUnit) {
    return Stream.of(values()).filter(unit -> unit.name().equals(jdkTimeUnit.name())).findFirst();
  }

}