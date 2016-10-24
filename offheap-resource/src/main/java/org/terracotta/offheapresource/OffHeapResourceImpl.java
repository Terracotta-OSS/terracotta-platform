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
package org.terracotta.offheapresource;

import java.util.concurrent.atomic.AtomicLong;

/**
 * An implementation of {@link OffHeapResource}.
 */
class OffHeapResourceImpl implements OffHeapResource {

  private final AtomicLong remaining;
  private final long capacity;

  /**
   * Creates a resource of the given initial size.
   *
   * @param size size of the resource
   * @throws IllegalArgumentException if the size is negative
   */
  OffHeapResourceImpl(long size) throws IllegalArgumentException {
    if (size < 0) {
      throw new IllegalArgumentException("Resource size cannot be negative");
    } else {
      this.capacity = size;
      this.remaining = new AtomicLong(size);
    }
  }

  /**
   * {@inheritDoc}
   * @throws IllegalArgumentException {@inheritDoc}
   */
  @Override
  public boolean reserve(long size) throws IllegalArgumentException {
    if (size < 0) {
      throw new IllegalArgumentException("Reservation size cannot be negative");
    } else {
      for (long current = remaining.get(); current >= size; current = remaining.get()) {
        if (remaining.compareAndSet(current, current - size)) {
          return true;
        }
      }
      return false;
    }
  }

  /**
   * {@inheritDoc}
   * @throws IllegalArgumentException {@inheritDoc}
   */
  @Override
  public void release(long size) throws IllegalArgumentException {
    if (size < 0) {
      throw new IllegalArgumentException("Released size cannot be negative");
    } else {
      remaining.addAndGet(size);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long available() {
    return remaining.get();
  }

  @Override
  public long capacity() {
    return capacity;
  }
}
