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
import java.util.function.Function;

/**
 * @author Mathieu Carbou
 */
public class Tuple2<T1, T2> {
  public final T1 t1;
  public final T2 t2;

  private Tuple2(T1 t1, T2 t2) {
    this.t1 = t1;
    this.t2 = t2;
  }

  public static <T1, T2> Tuple2<T1, T2> tuple2(T1 t1, T2 t2) {
    return new Tuple2<>(t1, t2);
  }

  public T1 getT1() {
    return t1;
  }

  public T2 getT2() {
    return t2;
  }

  public Optional<T1> findT1() {
    return Optional.ofNullable(t1);
  }

  public Optional<T2> findT2() {
    return Optional.ofNullable(t2);
  }

  public <R1, R2> Tuple2<R1, R2> map(Function<T1, R1> fn1, Function<T2, R2> fn2) {
    return tuple2(fn1.apply(t1), fn2.apply(t2));
  }

  public Tuple2<T1, T2> ifAllPresent(Consumer<Tuple2<T1, T2>> c) {
    if (t1 != null && t2 != null) {
      c.accept(this);
    }
    return this;
  }

  public boolean allNulls() {
    return t1 == null && t2 == null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Tuple2<?, ?> tuple2 = (Tuple2<?, ?>) o;
    // Using deepEquals because the parameters can be arrays. deepEquals() calls equals() when the Object is not an array.
    return Objects.deepEquals(t1, tuple2.t1) && Objects.deepEquals(t2, tuple2.t2);
  }

  @Override
  public int hashCode() {
    return Objects.hash(t1, t2);
  }

  @Override
  public String toString() {
    return "(" + t1 + ", " + t2 + ")";
  }
}
