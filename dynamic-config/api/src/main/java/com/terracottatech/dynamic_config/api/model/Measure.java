/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Collection;
import java.util.EnumSet;

import static java.util.Objects.requireNonNull;

public class Measure<T extends Enum<T> & Unit<T>> implements Comparable<Measure<T>> {

  private final long quantity;

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type")
  @JsonSubTypes({
      @JsonSubTypes.Type(name = "TIME", value = TimeUnit.class),
      @JsonSubTypes.Type(name = "MEMORY", value = MemoryUnit.class),
  })
  private final T unit;

  public static <U extends Enum<U> & Unit<U>> Measure<U> of(long quantity, U type) {
    return new Measure<>(quantity, type);
  }

  public static <U extends Enum<U> & Unit<U>> Measure<U> zero(Class<U> unitType) {
    return of(0, unitType.getEnumConstants()[0].getBaseUnit());
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

    long quantity;
    try {
      quantity = Long.parseLong(quantityUnit.substring(0, i));
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
          .findFirst()
          .orElseThrow(() -> new IllegalArgumentException("Invalid measure: '" + quantityUnit + "'. <unit> must be one of " + validUnits + "."));
    }

    return Measure.of(quantity, unit);
  }

  @JsonCreator
  private Measure(@JsonProperty(value = "quantity", required = true) long quantity,
                  @JsonProperty(value = "unit", required = true) T unit) {
    this.quantity = quantity;
    this.unit = requireNonNull(unit);
    if (quantity < 0) {
      throw new IllegalArgumentException("Quantity measure cannot be negative");
    }
  }

  public long getQuantity() {
    return quantity;
  }

  public long getQuantity(T unit) {
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
    if (o == null || getClass() != o.getClass()) return false;

    Measure<?> measure = (Measure<?>) o;

    if (quantity != measure.quantity) return false;
    return unit.equals(measure.unit);
  }

  @Override
  public int hashCode() {
    int result = (int) (quantity ^ (quantity >>> 32));
    result = 31 * result + unit.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return quantity + unit.getShortName();
  }

  @Override
  public int compareTo(Measure<T> o) {
    return Long.compare(getQuantity(unit.getBaseUnit()), o.getQuantity(o.getUnit().getBaseUnit()));
  }
}
