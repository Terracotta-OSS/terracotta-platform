/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.common.struct;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * @author Mathieu Carbou
 */
public class Tuple3<T1, T2, T3> {
  public final T1 t1;
  public final T2 t2;
  public final T3 t3;

  private Tuple3(T1 t1, T2 t2, T3 t3) {
    this.t1 = t1;
    this.t2 = t2;
    this.t3 = t3;
  }

  public T1 getT1() {
    return t1;
  }

  public T2 getT2() {
    return t2;
  }

  public T3 getT3() {
    return t3;
  }

  public Optional<T1> findT1() {
    return Optional.ofNullable(t1);
  }

  public Optional<T2> findT2() {
    return Optional.ofNullable(t2);
  }

  public Optional<T3> findT3() {
    return Optional.ofNullable(t3);
  }

  public static <T1, T2, T3> Tuple3<T1, T2, T3> tuple3(T1 t1, T2 t2, T3 t3) {
    return new Tuple3<>(t1, t2, t3);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Tuple3<?, ?, ?> tuple3 = (Tuple3<?, ?, ?>) o;
    // Using deepEquals because the parameters can be arrays. deepEquals() calls equals() when the Object is not an array.
    return Objects.deepEquals(t1, tuple3.t1) && Objects.deepEquals(t2, tuple3.t2) && Objects.deepEquals(t3, tuple3.t3);
  }

  @Override
  public int hashCode() {
    return Objects.hash(t1, t2, t3);
  }

  @Override
  public String toString() {
    return "(" + t1 + ", " + t2 + ", " + t3 + ")";
  }

  public Tuple3<T1, T2, T3> ifAllPresent(Consumer<Tuple3<T1, T2, T3>> c) {
    if (t1 != null && t2 != null && t3 != null) {
      c.accept(this);
    }
    return this;
  }
}
