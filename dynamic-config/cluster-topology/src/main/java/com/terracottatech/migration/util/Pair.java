/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.migration.util;

import java.util.Objects;

public final class Pair<T, U> {

  final T one;
  final U another;

  public Pair(T t, U u) {
    one = t;
    another = u;
  }

  public T getOne() {
    return one;
  }

  public U getAnother() {
    return another;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final Pair<?, ?> pair = (Pair<?, ?>)o;
    return Objects.equals(one, pair.one) &&
           Objects.equals(another, pair.another);
  }

  @Override
  public int hashCode() {
    return Objects.hash(one, another);
  }
}