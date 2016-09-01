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
package org.terracotta.management.service.monitoring;

import com.tc.classloader.CommonComponent;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

/**
 * @author Mathieu Carbou
 */
@CommonComponent
public interface ReadOnlyBuffer<V> {

  /**
   * Reads next available value from the buffer, waiting for it if not yet available
   */
  V take() throws InterruptedException;

  /**
   * Reads next available value from the buffer, waiting for it if not yet available
   */
  V take(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException;

  /**
   * Reads next available value from the buffer, or null if none available
   */
  V read();

  /**
   * Reads the estimated buffer size
   */
  int size();

  /**
   * Check if the buffer is empty
   */
  boolean isEmpty();

  /**
   * Returns a reading stream, like and sort of input stream on all the available values coming in this buffer.
   * This is a destructive operation since all values are read and cannot be read again.
   */
  Stream<V> stream();

  void clear();

}
