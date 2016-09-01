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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

/**
 * @author Mathieu Carbou
 */
public class TypedReadWriteBuffer<V> implements ReadWriteBuffer<V> {

  private final ReadWriteBuffer<Object> buffer;
  private final Class<V> type;

  @SuppressWarnings("unchecked")
  public TypedReadWriteBuffer(ReadWriteBuffer<?> buffer, Class<V> type) {
    this.buffer = (ReadWriteBuffer<Object>) buffer;
    this.type = type;
  }

  @Override
  public void clear() {
    buffer.clear();
  }

  @Override
  public void put(V value) {
    buffer.put(type.cast(value));
  }

  @Override
  public boolean isEmpty() {return buffer.isEmpty();}

  @Override
  public V read() {
    return type.cast(buffer.read());
  }

  @Override
  public int size() {return buffer.size();}

  @Override
  public Stream<V> stream() {
    return buffer.stream().map(type::cast);
  }

  @Override
  public V take() throws InterruptedException {
    return type.cast(buffer.take());
  }

  @Override
  public V take(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
    return type.cast(buffer.take(timeout, unit));
  }

}
