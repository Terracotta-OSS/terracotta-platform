/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Objects;

import static java.util.Comparator.comparing;
import static java.util.Objects.requireNonNull;

public class Measure<T extends Enum<T> & Unit<T>> implements Comparable<Measure<T>> {

  private final BigInteger quantity;

  private final T unit;

  public static <U extends Enum<U> & Unit<U>> Measure<U> of(long quantity, U type) {
    return new Measure<>(BigInteger.valueOf(quantity), type);
  }

  public static <U extends Enum<U> & Unit<U>> Measure<U> of(BigInteger quantity, U type) {
    return new Measure<>(quantity, type);
  }

  public static <U extends Enum<U> & Unit<U>> Measure<U> zero(Class<U> unitType) {
    return of(BigInteger.ZERO, unitType.getEnumConstants()[0].getBaseUnit());
  }

  public static <U extends Enum<U> & Unit<U>> Measure<U> parse(String quantityUnit, Class<U> unitType) throws IllegalArgumentException {
    return parse(quantityUnit, unitType, EnumSet.allOf(unitType));
  }

  public static <U extends Enum<U> & Unit<U>> Measure<U> parse(String quantityUnit, Class<U> unitType, Collection<U> validUnits) throws IllegalArgumentException {
    requireNonNull(quantityUnit);
    requireNonNull(unitType);

    // both quantity and unit are missing
    if (quantityUnit.isEmpty()) {
      throw new IllegalArgumentException("Invalid measure. <quantity><unit> are missing.");
    }

    char[] chars = quantityUnit.toCharArray();
    if (chars[0] == '-') {
      throw new IllegalArgumentException("Quantity measure cannot be negative");
    }

    // we are ordering the units because we first need to check
    // if the string ends with MB before checking if the string
    // ends with B (i.e. when using memory units).
    // Also, minus (-) is used to compare to not use .revered() which
    // is loosing the type information
    ArrayList<U> list = new ArrayList<>(validUnits);
    list.sort(comparing(u -> -u.getShortName().length()));

    U foundUnit = null; // XB
    for (U validUnit : list) {
      if (quantityUnit.endsWith(validUnit.getShortName())) {
        foundUnit = validUnit;
        break;
      }
    }
    if (foundUnit == null) {
      throw new IllegalArgumentException("Invalid measure: '" + quantityUnit + "'. <unit> is missing or not recognized. It must be one of " + validUnits + ".");
    }

    quantityUnit = quantityUnit.substring(0, quantityUnit.length() - foundUnit.getShortName().length());

    BigInteger quantity;
    try {
      quantity = new BigInteger(quantityUnit);
    } catch (NumberFormatException e) {
      // quantity is not a number
      throw new IllegalArgumentException("Invalid measure: '" + quantityUnit + "'. <quantity> must be a positive integer.");
    }

    return Measure.of(quantity, foundUnit);
  }

  protected Measure(BigInteger quantity, T unit) {
    this.quantity = quantity;
    this.unit = requireNonNull(unit);
    if (quantity.signum() == -1) {
      throw new IllegalArgumentException("Quantity measure cannot be negative");
    }
  }

  public long getQuantity() {
    return quantity.longValueExact();
  }

  public long getQuantity(T unit) {
    return unit.convert(this.quantity, this.unit).longValueExact();
  }

  public BigInteger getExactQuantity() {
    return quantity;
  }

  public BigInteger getExactQuantity(T unit) {
    return unit.convert(this.quantity, this.unit);
  }

  public Measure<T> to(T unit) {
    return Measure.of(getQuantity(unit), unit);
  }

  public T getUnit() {
    return unit;
  }

  public Measure<T> add(long quantity, T unit) {
    return add(BigInteger.valueOf(quantity), unit);
  }

  public Measure<T> add(BigInteger quantity, T unit) {
    return add(this.unit.convert(quantity, unit));
  }

  public Measure<T> add(long amount) {
    return add(BigInteger.valueOf(amount));
  }

  public Measure<T> add(BigInteger amount) {
    return Measure.of(this.quantity.add(amount), this.unit);
  }

  public Measure<T> multiply(long factor) {
    return multiply(BigInteger.valueOf(factor));
  }

  public Measure<T> multiply(BigInteger factor) {
    return Measure.of(this.quantity.multiply(factor), this.unit);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Measure)) return false;
    Measure<?> measure = (Measure<?>) o;
    return Objects.equals(getExactQuantity(), measure.getExactQuantity())
        && Objects.equals(getUnit(), measure.getUnit());
  }

  @Override
  public int hashCode() {
    return Objects.hash(quantity, unit);
  }

  @Override
  public String toString() {
    return quantity + unit.getShortName();
  }

  @Override
  public int compareTo(Measure<T> o) {
    return getExactQuantity(unit.getBaseUnit()).compareTo(o.getExactQuantity(o.getUnit().getBaseUnit()));
  }
}
