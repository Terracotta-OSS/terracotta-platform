/*
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is OffHeap Resource.
 *
 * The Initial Developer of the Covered Software is
 * Terracotta, Inc., a Software AG company
 */

package org.terracotta.offheapresource;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents an offheap resource, providing a reservation system that can be
 * used to control the combined memory usage of participating consumers.
 * <p>
 * Reservation and release calls perform no allocations, and therefore rely on
 * the cooperation of callers to achieve control over the 'real' resource usage.
 */
public class OffHeapResource {

  private final AtomicLong remaining;

  /**
   * Creates a resource of the given initial size.
   *
   * @param size size of the resource
   * @throws IllegalArgumentException if the size is negative
   */
  OffHeapResource(long size) throws IllegalArgumentException {
    if (size < 0) {
      throw new IllegalArgumentException("Resource size cannot be negative");
    } else {
      this.remaining = new AtomicLong(size);
    }
  }

  /**
   * Reserves the given amount of this resource.
   * <p>
   * This method <em>performs no allocation</em>.  It is simply a reservation
   * that the consumer agrees to bind by the result of.  A {@code false} return
   * should mean the caller refrains from performing any associated allocation.
   *
   * @param size reservation size
   * @return {@code true} if the reservation succeeded
   * @throws IllegalArgumentException if the reservation size is negative
   */
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
   * Releases the given amount of resource back to this pool.
   *
   * @param size release size
   * @throws IllegalArgumentException if the release size is negative
   */
  public void release(long size) throws IllegalArgumentException {
    if (size < 0) {
      throw new IllegalArgumentException("Released size cannot be negative");
    } else {
      remaining.addAndGet(size);
    }
  }

  /**
   * Returns the size of the remaining resource that can be reserved.
   *
   * @return the remaining resource size
   */
  public long available() {
    return remaining.get();
  }
}
