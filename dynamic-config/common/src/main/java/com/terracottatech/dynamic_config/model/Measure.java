/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

public class Measure<T extends Enum<T>> {
  private final long quantity;
  @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
  private final T type;

  @JsonCreator
  public static <U extends Enum<U>> Measure<U> of(
      @JsonProperty("quantity") long quantity,
      @JsonProperty("type") U type) {
    return new Measure<>(quantity, type);
  }

  private Measure(long quantity, T type) {
    this.quantity = quantity;
    this.type = type;
  }

  public long getQuantity() {
    return quantity;
  }

  public T getType() {
    return type;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Measure<?> measure = (Measure<?>) o;

    if (quantity != measure.quantity) return false;
    return type.equals(measure.type);
  }

  @Override
  public int hashCode() {
    int result = (int) (quantity ^ (quantity >>> 32));
    result = 31 * result + type.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "Measure{" +
        "quantity=" + quantity +
        ", type=" + type +
        '}';
  }
}
