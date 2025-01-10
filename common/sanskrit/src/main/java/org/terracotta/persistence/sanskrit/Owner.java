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
/**
 * Not thread-safe
 */
package org.terracotta.persistence.sanskrit;

/**
 * This class provides a way for resources that are made with a try-with-resources block to escape from the
 * try-with-resources block without being closed. This is useful when AutoCloseable objects are being built: if there
 * is an error during construction we want the resources to be closed correctly, but on success we do not want to close
 * those resources.
 *
 * @param <T> the type of AutoCloseable object that may be allowed to escape the try-with-resources block
 * @param <E> the type of Exception that is thrown by the close() method on the AutoCloseable object.
 */
public class Owner<T extends AutoCloseable, E extends Exception> implements AutoCloseable {
  private final T underlying;
  private final Class<E> exceptionClass;
  private boolean released;

  public static <T extends AutoCloseable, E extends Exception> Owner<T, E> own(T underlying, Class<E> exceptionClass) {
    return new Owner<>(underlying, exceptionClass);
  }

  private Owner(T underlying, Class<E> exceptionClass) {
    this.underlying = underlying;
    this.exceptionClass = exceptionClass;
  }

  public T borrow() {
    return underlying;
  }

  public T release() {
    released = true;
    return underlying;
  }

  @Override
  public void close() throws E {
    if (!released && underlying != null) {
      try {
        underlying.close();
      } catch (Exception e) {
        if (e instanceof RuntimeException) {
          throw (RuntimeException) e;
        }

        if (exceptionClass.isAssignableFrom(e.getClass())) {
          throw uncheckedExceptionCast(e);
        }

        throw new RuntimeException("Unexpected exception type. Expected: " + exceptionClass + ", found: " + e.getClass(), e);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private E uncheckedExceptionCast(Exception e) {
    return (E) e;
  }
}
