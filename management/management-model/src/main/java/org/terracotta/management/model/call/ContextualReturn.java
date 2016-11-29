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
package org.terracotta.management.model.call;

import org.terracotta.management.model.Objects;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.context.Contextual;

import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;

/**
 * This class holds the results of the calls on each context.
 * <p>
 * If a call was not possible to make on a context, because the context was not supported or the capability not found,
 * then the method {@link #hasExecuted()} will return false.
 * <p>
 * You an call {@link #getValue()} only if there has been a result, event if it is null.
 *
 * @author Mathieu Carbou
 */
public final class ContextualReturn<T> implements Contextual {

  private static final long serialVersionUID = 1;

  private final T value;
  private Context context;
  private final String capability;
  private final String methodName;
  private final ExecutionException error;
  private final boolean executed;

  private ContextualReturn(String capability, Context context, String methodName, T value, ExecutionException error, boolean executed) {
    this.methodName = methodName;
    this.value = value;
    this.context = Objects.requireNonNull(context);
    this.capability = Objects.requireNonNull(capability);
    this.error = error;
    this.executed = executed;
  }

  /**
   * @return true if the management call has been executed (if a provider has been found to execute this call), false otherwise
   */
  public boolean hasExecuted() {
    return executed;
  }

  public boolean errorThrown() {
    return error != null;
  }

  /**
   * @return The returned value, might be null
   * @throws NoSuchElementException If the management call was not able to execute because a provider was not found
   * @throws ExecutionException     If the management call launched an exception
   */
  public T getValue() throws NoSuchElementException, ExecutionException {
    if (!hasExecuted()) {
      throw new NoSuchElementException("Query has not been executed: capability=" + capability + ", method=" + methodName + ", context=" + context);
    }
    if (error != null) {
      throw error;
    }
    return value;
  }

  @Override
  public void setContext(Context context) {
    this.context = Objects.requireNonNull(context);
  }

  @Override
  public Context getContext() {
    return context;
  }

  public static <T> ContextualReturn<T> of(String capability, Context context, String methodName, T result) {
    return new ContextualReturn<T>(capability, context, methodName, result, null, true);
  }

  @SuppressWarnings("unchecked")
  public static <T> ContextualReturn<T> notExecuted(String capability, Context context, String methodName) {
    return new ContextualReturn<T>(capability, context, methodName, null, null, false);
  }

  public static <T> ContextualReturn<T> error(String capability, Context context, String methodName, ExecutionException throwable) {
    return new ContextualReturn<T>(capability, context, methodName, null, throwable, true);
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
        ", method='" + methodName + '\'' +
        ", context=" + context +
        ", executed=" + executed +
        ", error=" + (error != null) +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ContextualReturn<?> that = (ContextualReturn<?>) o;

    if (executed != that.executed) return false;
    if (value != null ? !value.equals(that.value) : that.value != null) return false;
    if (!context.equals(that.context)) return false;
    if (!capability.equals(that.capability)) return false;
    if (!methodName.equals(that.methodName)) return false;
    return error != null ? error.equals(that.error) : that.error == null;

  }

  @Override
  public int hashCode() {
    int result = value != null ? value.hashCode() : 0;
    result = 31 * result + context.hashCode();
    result = 31 * result + capability.hashCode();
    result = 31 * result + methodName.hashCode();
    result = 31 * result + (error != null ? error.hashCode() : 0);
    result = 31 * result + (executed ? 1 : 0);
    return result;
  }

}
