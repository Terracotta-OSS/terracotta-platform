/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.common.struct;

public enum MemoryUnit implements Unit<MemoryUnit> {
  B(0),
  KB(10),
  MB(20),
  GB(30),
  TB(40),
  PB(50);

  private final int bitShift;
  private final long mask;

  MemoryUnit(int bitShift) {
    this.bitShift = bitShift;
    this.mask = -1L << (63 - bitShift);
  }

  @Override
  public String getShortName() {
    return name();
  }

  @Override
  public long convert(long quantity, MemoryUnit unit) {
    if (this == unit) {
      return quantity;
    }
    return unit.toBytes(quantity) / this.toBytes(1);
  }

  @Override
  public MemoryUnit getBaseUnit() {
    return B;
  }

  @Override
  public String toString() {
    return getShortName();
  }

  public long toBytes(long quantity) {
    if (bitShift == 0) {
      return quantity;
    }

    if (quantity == Long.MIN_VALUE) {
      throw new IllegalArgumentException("Byte count is too large: " + quantity + this);
    }

    if (quantity < 0) {
      return -1 * toBytes(-1 * quantity);
    }

    if (quantity == 0) {
      return 0;
    }

    if ((quantity & mask) != 0) {
      throw new IllegalArgumentException("Byte count is too large: " + quantity + this);
    }

    return quantity << bitShift;
  }

  public static MemoryUnit parse(String s) {
    for (MemoryUnit value : values()) {
      if (value.name().equalsIgnoreCase(s)) {
        return value;
      }
    }
    throw new IllegalArgumentException(s);
  }
}