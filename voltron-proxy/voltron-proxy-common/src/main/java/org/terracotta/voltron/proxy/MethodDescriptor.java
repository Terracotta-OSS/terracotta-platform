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
package org.terracotta.voltron.proxy;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author Mathieu Carbou
 */
public final class MethodDescriptor {

  private final boolean async;
  private final Class<?> messageType;
  private final Method method;
  private final Async.Ack ack;
  private final ExecutionStrategy.Location location;
  private final int concurrencyKey;
  private final long delayMs;
  private final long frequencyMs;

  private MethodDescriptor(Method method) {
    this.method = method;

    // @Async
    Async asyncAnnot = method.getAnnotation(Async.class);
    async = asyncAnnot != null;
    if (async) {
      // @Async required a Future
      if (method.getReturnType() != Future.class) {
        throw new IllegalStateException("@Async requires a Future as a return type on method: " + method);
      }
      ack = asyncAnnot.value();
      Type returnType = method.getGenericReturnType();
      messageType = returnType instanceof Class<?> ?
          Object.class : // this is the case where a Future is returned with no given generic type
          determineRawType(((ParameterizedType) returnType).getActualTypeArguments()[0]);
    } else {
      ack = Async.Ack.NONE;
      messageType = method.getReturnType();
    }

    // @ExecutionStrategy
    ExecutionStrategy executionStrategy = method.getAnnotation(ExecutionStrategy.class);
    location = executionStrategy == null ? ExecutionStrategy.Location.BOTH : executionStrategy.location();

    // @ConcurrencyStrategy
    ConcurrencyStrategy concurrencyStrategy = method.getAnnotation(ConcurrencyStrategy.class);
    concurrencyKey = concurrencyStrategy == null ? ConcurrencyStrategy.MANAGEMENT_KEY : concurrencyStrategy.key();

    EntityMessenger.Delayed delayed = method.getAnnotation(EntityMessenger.Delayed.class);
    delayMs = delayed == null ? -1 : delayed.value() > 0 ? TimeUnit.MILLISECONDS.convert(delayed.value(), delayed.unit()) : -1;

    EntityMessenger.Frequency frequency = method.getAnnotation(EntityMessenger.Frequency.class);
    frequencyMs = frequency == null ? -1 : frequency.value() > 0 ? TimeUnit.MILLISECONDS.convert(frequency.value(), frequency.unit()) : -1;
  }

  public long getDelayMs() {
    return delayMs;
  }

  public long getFrequencyMs() {
    return frequencyMs;
  }

  public int getConcurrencyKey() {
    return concurrencyKey;
  }

  public ExecutionStrategy.Location getExecutionLocation() {
    return location;
  }

  public Async.Ack getAck() {
    return ack;
  }

  public boolean isAsync() {
    return async;
  }

  public Class<?> getMessageType() {
    return messageType;
  }

  public static MethodDescriptor of(Method method) {
    return new MethodDescriptor(method);
  }

  public String toGenericString() {
    return method.toGenericString();
  }

  @Override
  public String toString() {
    return toGenericString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MethodDescriptor that = (MethodDescriptor) o;
    return method.equals(that.method);
  }

  @Override
  public int hashCode() {
    return method.hashCode();
  }

  public Class<?>[] getParameterTypes() {
    return method.getParameterTypes();
  }

  public Annotation[][] getParameterAnnotations() {
    return method.getParameterAnnotations();
  }

  public Object invoke(Object target, Object... args) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
    Object ret = method.invoke(target, args);
    if (async) {
      try {
        ret = ((Future<?>) ret).get();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new InvocationTargetException(e);
      } catch (ExecutionException e) {
        throw new InvocationTargetException(e.getCause());
      }
    }
    return ret;
  }

  public Method getMethod() {
    return method;
  }

  private static Class<?> determineRawType(Type type) {
    if (type instanceof Class<?>) {
      return (Class<?>) type;
    }
    if (type instanceof ParameterizedType) {
      return determineRawType(((ParameterizedType) type).getRawType());
    }
    if (type instanceof WildcardType) {
      return determineRawType(((WildcardType) type).getUpperBounds()[0]);
    }
    if (type instanceof GenericArrayType) {
      Class<?> rawComponentType = determineRawType(((GenericArrayType) type).getGenericComponentType());
      return Array.newInstance(rawComponentType, 0).getClass();
    }
    if (type instanceof TypeVariable<?>) {
      return determineRawType(((TypeVariable) type).getBounds()[0]);
    }
    throw new IllegalStateException("Unsupported type: " + type);
  }

}
