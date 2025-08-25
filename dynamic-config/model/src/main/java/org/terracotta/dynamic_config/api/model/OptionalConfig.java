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
package org.terracotta.dynamic_config.api.model;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;

/**
 * @author Mathieu Carbou
 */
public class OptionalConfig<T> {

  private final Setting setting;
  private final T value;

  private OptionalConfig(Setting setting, T value) {
    this.setting = requireNonNull(setting);
    this.value = value;
  }

  /**
   * Returns the configured value, throwing if null
   */
  public T get() throws NoSuchElementException {
    if (value == null) {
      throw new NoSuchElementException("No value present");
    }
    return value;
  }

  public boolean isConfigured() {
    return value != null;
  }

  public void ifConfigured(Consumer<? super T> consumer) {
    if (value != null) {
      consumer.accept(value);
    }
  }

  public T orElse(T other) {
    return value != null ? value : other;
  }

  /**
   * Returns the configured value, or the default one if not configured. Can return null.
   */
  public T orDefault() {
    return orElseGet(setting::getDefaultValue);
  }

  public T orElseGet(Supplier<? extends T> other) {
    return value != null ? value : other.get();
  }

  public <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
    if (value != null) {
      return value;
    } else {
      throw exceptionSupplier.get();
    }
  }

  public boolean is(T other) {
    return Objects.equals(value, other);
  }

  public Optional<T> filter(Predicate<? super T> predicate) {
    Objects.requireNonNull(predicate);
    return value == null ? empty() : predicate.test(value) ? Optional.of(value) : empty();
  }

  public <U> Optional<U> map(Function<? super T, ? extends U> mapper) {
    Objects.requireNonNull(mapper);
    return value == null ? empty() : Optional.ofNullable(mapper.apply(value));
  }

  public <U> Optional<U> flatMap(Function<? super T, Optional<U>> mapper) {
    Objects.requireNonNull(mapper);
    return value == null ? empty() : Objects.requireNonNull(mapper.apply(value));
  }

  public Optional<T> asOptional() {
    return Optional.ofNullable(value);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof OptionalConfig)) {
      return false;
    }
    OptionalConfig<?> other = (OptionalConfig<?>) obj;
    return Objects.equals(value, other.value)
        && Objects.equals(setting, other.setting);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value, setting);
  }

  @Override
  public String toString() {
    return setting + "=" + Setting.toProperty(value).orElse("<unset>");
  }

  public static <T> OptionalConfig<T> of(Setting setting, T value) {
    return new OptionalConfig<>(setting, value);
  }
}
