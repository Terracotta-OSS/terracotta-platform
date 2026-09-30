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
package org.terracotta.offheapresource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.offheapresource.management.OffHeapResourceBinding;
import org.terracotta.tripwire.MemoryMonitor;
import org.terracotta.tripwire.TripwireFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * An implementation of {@link OffHeapResource}.
 */
final class OffHeapResourceImpl implements OffHeapResource, AutoCloseable {

  private static final Logger LOGGER = LoggerFactory.getLogger(OffHeapResourceImpl.class);

  private static final String MESSAGE_PROPERTIES_RESOURCE_NAME = "/offheap-message.properties";
  private static final String OFFHEAP_INFO_KEY = "offheap.info";
  private static final String OFFHEAP_WARN_KEY = "offheap.warn";
  private static final String DEFAULT_MESSAGE = "Offheap allocation for resource \"{}\" reached {}%, you may run out of memory if allocation continues.";
  private static final Properties MESSAGE_PROPERTIES;
  private final Map<UUID, OffHeapUsageListener> listenerMap = new ConcurrentHashMap<>();

  static {
    Properties defaults = new Properties();
    defaults.setProperty(OFFHEAP_INFO_KEY, DEFAULT_MESSAGE);
    defaults.setProperty(OFFHEAP_WARN_KEY, DEFAULT_MESSAGE);
    MESSAGE_PROPERTIES = new Properties(defaults);
    boolean loaded = false;
    try (InputStream resource = OffHeapResourceImpl.class.getResourceAsStream(MESSAGE_PROPERTIES_RESOURCE_NAME)) {
      if (resource != null) {
        MESSAGE_PROPERTIES.load(resource);
        loaded = true;
      }
    } catch (IOException e) {
      LOGGER.debug("Exception loading {}", MESSAGE_PROPERTIES_RESOURCE_NAME, e);
    } finally {
      if (!loaded) {
        LOGGER.info("Unable to load {}, will be using default messages.", MESSAGE_PROPERTIES_RESOURCE_NAME);
      }

    }
  }

  /**
   * Used bytes. Contended on by every reserving/releasing thread; kept as a bare
   * primitive CAS target so reserve/release perform no object allocation.
   */
  private final AtomicLong used = new AtomicLong(0L);

  /**
   * Capacity in bytes. Only setCapacity (a rare admin operation) modifies it.
   */
  private volatile long capacity;

  /**
   * Lowest threshold among listeners that have not yet fired. Occupancy strictly
   * below this value means a rising transition can fire nothing. +Inf when no
   * unfired listener exists.
   */
  private volatile float minUnfiredThreshold = Float.POSITIVE_INFINITY;

  /**
   * Highest threshold among listeners that are currently fired. Occupancy at or
   * above this value means a falling transition can fire nothing. -Inf when no
   * fired listener exists.
   */
  private volatile float maxFiredThreshold = Float.NEGATIVE_INFINITY;

  /**
   * Serializes listener flag changes / listener-set mutations with the watermark
   * recomputation that depends on them. Never held on the reserve/release fast path.
   */
  private final Object listenerLock = new Object();

  private final String identifier;
  private final CapacityChangeHandler onCapacityChanged;
  private final OffHeapResourceBinding managementBinding;
  private final MemoryMonitor monitor;

  /**
   * Creates a resource of the given initial size.
   *
   * @param identifier
   * @param size size of the resource
   * @param onReservationThresholdReached event consumer - will receive events regarding usage thresholds
   * @param onCapacityChanged event consumer - will receive an event when the capacity changes
   * @throws IllegalArgumentException if the size is negative
   */
  OffHeapResourceImpl(String identifier, long size, Consumer<OffHeapUsageEvent> onReservationThresholdReached, CapacityChangeHandler onCapacityChanged) throws IllegalArgumentException {
    this.onCapacityChanged = onCapacityChanged;
    this.managementBinding = new OffHeapResourceBinding(identifier, this);
    if (size < 0) {
      throw new IllegalArgumentException("Resource size cannot be negative");
    }

    this.capacity = size;
    this.identifier = identifier;
    monitor = TripwireFactory.createMemoryMonitor(identifier);
    monitor.register();
    addUsageListener(UUID.randomUUID(), 0.9f, onReservationThresholdReached);
    addUsageListener(UUID.randomUUID(), 0.75f, onReservationThresholdReached);
  }

  /**
   * Creates a resource of the given initial size.
   *
   * @param identifier
   * @param size size of the resource
   * @param onReservationThresholdReached event consumer - will receive events regarding usage thresholds
   * @throws IllegalArgumentException if the size is negative
   */
  OffHeapResourceImpl(String identifier, long size, Consumer<OffHeapUsageEvent> onReservationThresholdReached) throws IllegalArgumentException {
    this(identifier, size, onReservationThresholdReached, (r, o, n) -> {});
  }


  /**
   * Creates a resource of the given initial size.
   *
   * @param identifier
   * @param size size of the resource
   * @throws IllegalArgumentException if the size is negative
   */
  OffHeapResourceImpl(String identifier, long size) throws IllegalArgumentException {
    this(identifier, size, (p) -> {});
  }

  public OffHeapResourceBinding getManagementBinding() {
    return managementBinding;
  }

  @Override
  public void close() {
    monitor.unregister();
  }

  /**
   * {@inheritDoc}
   * @throws IllegalArgumentException {@inheritDoc}
   */
  @Override
  public boolean reserve(long size) throws IllegalArgumentException {
    if (size < 0) {
      throw new IllegalArgumentException("Reservation size cannot be negative");
    }

    long prevUsed;
    long newUsed;
    long currentCapacity;
    do {
      prevUsed = used.get();
      newUsed = prevUsed + size;
      currentCapacity = capacity;
      if (newUsed > currentCapacity) {
        return false;
      }
    } while (!used.compareAndSet(prevUsed, newUsed));

    stateUpdated(prevUsed, newUsed, currentCapacity, currentCapacity);
    return true;
  }

  private void stateUpdated(long prevUsed, long newUsed, long prevCapacity, long newCapacity) {
    if (newUsed > prevUsed || newCapacity < prevCapacity) {
      // rising transition: used increased or capacity decreased
      float occupancy = (newUsed * 1.0f) / newCapacity;
      if (occupancy >= minUnfiredThreshold) {
        checkRisingThresholds(occupancy, newUsed, newCapacity);
      }
    } else if (newUsed < prevUsed || newCapacity > prevCapacity) {
      // falling transition: used decreased or capacity increased
      float occupancy = (newUsed * 1.0f) / newCapacity;
      if (occupancy < maxFiredThreshold) {
        checkFallingThresholds(occupancy, newUsed, newCapacity);
      }
    }

    monitor.sample(newCapacity - newUsed, newUsed);
  }

  private void checkRisingThresholds(float occupancy, long used, long capacity) {
    synchronized (listenerLock) {
      OffHeapUsageEvent offHeapUsageEvent = null;
      for (OffHeapUsageListener offHeapUsageListener : listenerMap.values()) {
        if (!offHeapUsageListener.isFired() && (Float.compare(offHeapUsageListener.getThreshold(), occupancy) <= 0)) {
          if (offHeapUsageEvent == null) {
            offHeapUsageEvent = new OffHeapUsageEventImpl(used, capacity - used, capacity, OffHeapUsageEventType.RISING);
          }
          if (Float.compare(offHeapUsageListener.getThreshold(), 0.9f) == 0) {
            LOGGER.warn(MESSAGE_PROPERTIES.getProperty(OFFHEAP_WARN_KEY), identifier, (used * 100L) / capacity);
          } else if (Float.compare(offHeapUsageListener.getThreshold(), 0.75f) == 0) {
            LOGGER.info(MESSAGE_PROPERTIES.getProperty(OFFHEAP_INFO_KEY), identifier, (used * 100L) / capacity);
          }
          offHeapUsageListener.getConsumer().accept(offHeapUsageEvent);
          offHeapUsageListener.setFiringStatus(true);
        }
      }
      recomputeThresholdWatermarks();
    }
  }

  private void checkFallingThresholds(float occupancy, long used, long capacity) {
    synchronized (listenerLock) {
      OffHeapUsageEvent offHeapUsageEvent = null;
      for (OffHeapUsageListener offHeapUsageListener : listenerMap.values()) {
        if (offHeapUsageListener.isFired() && (Float.compare(offHeapUsageListener.getThreshold(), occupancy) > 0)) {
          if (offHeapUsageEvent == null) {
            offHeapUsageEvent = new OffHeapUsageEventImpl(used, capacity - used, capacity, OffHeapUsageEventType.FALLING);
          }
          if (Float.compare(offHeapUsageListener.getThreshold(), 0.75f) == 0) {
            LOGGER.info(MESSAGE_PROPERTIES.getProperty(OFFHEAP_INFO_KEY), identifier, (used * 100L) / capacity);
          }
          offHeapUsageListener.getConsumer().accept(offHeapUsageEvent);
          offHeapUsageListener.setFiringStatus(false);
        }
      }
      recomputeThresholdWatermarks();
    }
  }

  private void recomputeThresholdWatermarks() {
    float minUnfired = Float.POSITIVE_INFINITY;
    float maxFired = Float.NEGATIVE_INFINITY;
    for (OffHeapUsageListener listener : listenerMap.values()) {
      if (listener.isFired()) {
        if (Float.compare(listener.getThreshold(), maxFired) > 0) {
          maxFired = listener.getThreshold();
        }
      } else if (Float.compare(listener.getThreshold(), minUnfired) < 0) {
        minUnfired = listener.getThreshold();
      }
    }
    minUnfiredThreshold = minUnfired;
    maxFiredThreshold = maxFired;
  }

  /**
   * {@inheritDoc}
   * @throws IllegalArgumentException {@inheritDoc}
   */
  @Override
  public void release(long size) throws IllegalArgumentException {
    if (size < 0) {
      throw new IllegalArgumentException("Released size cannot be negative");
    }

    long prevUsed = used.getAndAdd(-size);
    long currentCapacity = capacity;
    stateUpdated(prevUsed, prevUsed - size, currentCapacity, currentCapacity);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long available() {
    return capacity - used.get();
  }

  @Override
  public long capacity() {
    return capacity;
  }

  @Override
  public boolean setCapacity(long size) throws IllegalArgumentException {
    if (size < 0) {
      throw new IllegalArgumentException("New capacity size cannot be negative");
    }

    if (used.get() > size) {
      return false;
    }

    long previousCapacity = this.capacity;
    this.capacity = size;
    onCapacityChanged.onCapacityChanged(this, previousCapacity, size);
    stateUpdated(used.get(), used.get(), previousCapacity, size);
    return true;
  }

  @Override
  public void addUsageListener(UUID listenerUUID, float threshold, Consumer<OffHeapUsageEvent> consumer) {
    OffHeapUsageListener offHeapUsageListener = new OffHeapUsageListener(threshold, consumer);
    synchronized (listenerLock) {
      listenerMap.put(listenerUUID, offHeapUsageListener);
      // check for rising event if current usage already is above threshold.
      long used = this.used.get();
      long capacity = this.capacity;
      float occupancy = (used * 1.0f) / capacity;
      if ((Float.compare(offHeapUsageListener.getThreshold(), occupancy) <= 0)) {
        OffHeapUsageEvent offHeapUsageEvent = new OffHeapUsageEventImpl(used, capacity - used, capacity, OffHeapUsageEventType.RISING);
        offHeapUsageListener.getConsumer().accept(offHeapUsageEvent);
        offHeapUsageListener.setFiringStatus(true);
      }
      recomputeThresholdWatermarks();
    }
  }

  @Override
  public void removeUsageListener(UUID listenerUUID) throws IllegalArgumentException {
    synchronized (listenerLock) {
      if (listenerMap.remove(listenerUUID) == null) {
        throw new IllegalArgumentException("Unknown listener: " + listenerUUID);
      }
      recomputeThresholdWatermarks();
    }
  }
}
