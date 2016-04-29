package org.terracotta.offheapresource;

import com.tc.classloader.CommonComponent;

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
}
