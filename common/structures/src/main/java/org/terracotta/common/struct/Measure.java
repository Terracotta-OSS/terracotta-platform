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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.math.BigInteger;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

public class Measure<T extends Enum<T> & Unit<T>> implements Comparable<Measure<T>> {

  private final BigInteger quantity;

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type")
  @JsonSubTypes({
      @JsonSubTypes.Type(name = "TIME", value = TimeUnit.class),
      @JsonSubTypes.Type(name = "MEMORY", value = MemoryUnit.class),
  })
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
    return parse(quantityUnit, unitType, null, EnumSet.allOf(unitType));
  }

  public static <U extends Enum<U> & Unit<U>> Measure<U> parse(String quantityUnit, Class<U> unitType, U defaultUnit) throws IllegalArgumentException {
    return parse(quantityUnit, unitType, defaultUnit, EnumSet.allOf(unitType));
  }

  public static <U extends Enum<U> & Unit<U>> Measure<U> parse(String quantityUnit, Class<U> unitType, U defaultUnit, Collection<U> validUnits) throws IllegalArgumentException {
    requireNonNull(quantityUnit);
    requireNonNull(unitType);

    // bad default unit
    if (defaultUnit != null && !validUnits.contains(defaultUnit)) {
      throw new IllegalArgumentException("Default unit '" + defaultUnit.getShortName() + "' is not in the list of valid units " + validUnits + ".");
    }

    // both quantity and unit are missing
    if (quantityUnit.isEmpty()) {
      throw new IllegalArgumentException("Invalid measure. <quantity><unit> are missing.");
    }

    char[] chars = quantityUnit.toCharArray();
    if (chars[0] == '-') {
      throw new IllegalArgumentException("Quantity measure cannot be negative");
    }

    int i;
    for (i = 0; i < chars.length; i++) {
      if (!Character.isDigit(chars[i])) {
        break;
      }
    }

    // quantity is missing
    if (i == 0) {
      throw new IllegalArgumentException("Invalid measure: '" + quantityUnit + "'. <quantity> is missing. Measure should be specified in <quantity><unit> format.");
    }

    BigInteger quantity;
    try {
      quantity = new BigInteger(quantityUnit.substring(0, i));
    } catch (NumberFormatException e) {
      // quantity is not a number
      throw new IllegalArgumentException("Invalid measure: '" + quantityUnit + "'. <quantity> is not a valid number.");
    }

    U unit;
    if (i == quantityUnit.length()) {
      if (defaultUnit != null) {
        unit = defaultUnit;
      } else {
        // unit is missing
        throw new IllegalArgumentException("Invalid measure: '" + quantityUnit + "'. <unit> is missing. Measure should be specified in <quantity><unit> format.");
      }
    } else {
      String q = quantityUnit.substring(i);
      unit = validUnits.stream()
          .filter(u -> u.getShortName().equals(q))
          .findAny()
          .orElseThrow(() -> new IllegalArgumentException("Invalid measure: '" + quantityUnit + "'. <unit> must be one of " + validUnits + "."));
    }

    return Measure.of(quantity, unit);
  }

  @JsonCreator
  private Measure(@JsonProperty(value = "quantity", required = true) BigInteger quantity,
                  @JsonProperty(value = "unit", required = true) T unit) {
    this.quantity = quantity;
    this.unit = requireNonNull(unit);
    if (quantity.signum() == -1) {
      throw new IllegalArgumentException("Quantity measure cannot be negative");
    }
  }

  @JsonIgnore
  public long getQuantity() {
    return quantity.longValueExact();
  }

  public long getQuantity(T unit) {
    return unit.convert(this.quantity, this.unit).longValueExact();
  }

  @JsonProperty("quantity")
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
