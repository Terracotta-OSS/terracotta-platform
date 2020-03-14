/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.common.struct;

import java.math.BigInteger;

public enum MemoryUnit implements Unit<MemoryUnit> {
  B(0),
  KB(10),
  MB(20),
  GB(30),
  TB(40),
  PB(50);

  private final int bitShift;

  MemoryUnit(int bitShift) {
    this.bitShift = bitShift;
  }

  @Override
  public String getShortName() {
    return name();
  }

  @Override
  public BigInteger convert(BigInteger quantity, MemoryUnit unit) {
    return this == unit ? quantity : unit.toBytes(quantity).divide(this.toBytes(BigInteger.ONE));
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
    return toBytes(BigInteger.valueOf(quantity)).longValueExact();
  }

  public BigInteger toBytes(BigInteger quantity) {
    if (bitShift == 0) {
      return quantity;
    }

    if (quantity.signum() == -1) {
      final BigInteger minusOne = BigInteger.ONE.negate();
      return minusOne.multiply(toBytes(minusOne.multiply(quantity)));
    }

    if (quantity.equals(BigInteger.ZERO)) {
      return BigInteger.ZERO;
    }

    return quantity.shiftLeft(bitShift);
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