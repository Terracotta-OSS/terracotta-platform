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

import com.tc.classloader.CommonComponent;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Represents an offheap resource, providing a reservation system that can be
 * used to control the combined memory usage of participating consumers.
 * <p>
 * Reservation and release calls perform no allocations, and therefore rely on
 * the cooperation of callers to achieve control over the 'real' resource usage.
 */
@CommonComponent
public interface OffHeapResource {

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
  boolean reserve(long size) throws IllegalArgumentException;

  /**
   * Releases the given amount of resource back to this pool.
   *
   * @param size release size
   * @throws IllegalArgumentException if the release size is negative
   */
  void release(long size) throws IllegalArgumentException;

  /**
   * Returns the size of the remaining resource that can be reserved.
   *
   * @return the remaining resource size
   */
  long available();

  /**
   * @return the resource initial capacity
   */
  long capacity();

  /**
   * Set the capacity of this resource to a new value. The new value must be at
   * least as much as is currently reserved.
   *
   * @param size new capacity for the offheap resource
   * @return {code true} if the capacity was changed
   * @throws IllegalArgumentException if the new capacity is negative
   */
  boolean setCapacity(long size) throws IllegalArgumentException;

  void addUsageListener(UUID listenerUUID, float threshold, Consumer<OffHeapUsageEvent> consumer);

  void removeUsageListener(UUID listenerUUID) throws IllegalArgumentException;
}
