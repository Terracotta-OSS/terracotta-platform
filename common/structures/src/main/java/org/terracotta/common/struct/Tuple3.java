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
