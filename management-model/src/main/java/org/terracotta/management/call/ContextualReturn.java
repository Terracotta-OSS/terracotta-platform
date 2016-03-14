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
package org.terracotta.management.call;

import org.terracotta.management.Objects;
import org.terracotta.management.context.Context;

import java.io.Serializable;
import java.util.NoSuchElementException;

/**
 * This class holds the results of the calls on each context.
 * <p>
 * If a call was not possible to make on a context, because the context was not supported or the capability not found,
 * then the method {@link #hasValue()} will return false.
 * <p>
 * You an call {@link #getValue()} only if there has been a result, event if it is null.
 *
 * @author Mathieu Carbou
 */
public final class ContextualReturn<T extends Serializable> implements Serializable {

  private final T value;
  private final Context context;
  private final String capability;
  private final String methodName;

  private ContextualReturn(String capability, Context context, String methodName, T value) {
    this.methodName = methodName;
    this.value = value;
    this.context = Objects.requireNonNull(context);
    this.capability = Objects.requireNonNull(capability);
  }

  public boolean hasValue() {
    return value != Void.TYPE;
  }

  public T getValue() throws NoSuchElementException {
    if (!hasValue()) {
      throw new NoSuchElementException();
    }
    return value;
  }

  public Context getContext() {
    return context;
  }

  public static <T extends Serializable> ContextualReturn<T> of(String capability, Context context, String methodName, T result) {
    return new ContextualReturn<T>(capability, context, methodName, result);
  }

  @SuppressWarnings("unchecked")
  public static <T extends Serializable> ContextualReturn<T> empty(String capability, Context context, String methodName) {
    return new ContextualReturn<T>(capability, context, methodName, (T) Void.TYPE);
  }

  public String getCapability() {
    return capability;
  }

  public String getMethodName() {
    return methodName;
  }

  @Override
  public String toString() {
    return "ContextualReturn{" +
        "capability='" + capability + '\'' +
        "method='" + methodName + '\'' +
        ", context=" + context +
        ", value=" + value +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ContextualReturn<?> that = (ContextualReturn<?>) o;

    if (value != null ? !value.equals(that.value) : that.value != null) return false;
    if (!context.equals(that.context)) return false;
    if (!methodName.equals(that.methodName)) return false;
    return capability.equals(that.capability);

  }

  @Override
  public int hashCode() {
    int result = value != null ? value.hashCode() : 0;
    result = 31 * result + context.hashCode();
    result = 31 * result + methodName.hashCode();
    result = 31 * result + capability.hashCode();
    return result;
  }

}
