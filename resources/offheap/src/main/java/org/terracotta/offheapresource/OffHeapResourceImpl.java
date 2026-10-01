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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
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
   * Capacity in bytes. Written only by {@link #setCapacity(long)}, which holds
   * the {@link #capacityLock} write lock, excluding all usage changes while the
   * capacity is replaced — the two-word analogue of the single-state CAS this
   * class previously used. Readers take the read lock to observe a coherent
   * (capacity, used) pair.
   */
  private volatile long capacity;

  /**
   * Coordinates capacity changes (rare admin operations, exclusive) with
   * reservations/releases/reads (hot path, shared). Reserves and releases still
   * race each other freely on {@link #used}; only capacity mutations exclude
   * them. Fair, so a continuous stream of readers cannot starve the admin
   * capacity change.
   */
  private final ReentrantReadWriteLock capacityLock = new ReentrantReadWriteLock(true);

  /**
   * Threshold watermarks used to skip listener scans that provably cannot fire
   * an event. Published as a single immutable snapshot so readers always see a
   * coherent pair. Only mutated under {@link #listenerLock}.
   */
  private volatile ThresholdWatermarks watermarks = ThresholdWatermarks.NONE;

  /**
   * Serializes listener flag changes / listener-set mutations with the watermark
   * recomputation that depends on them. Never held while invoking user callbacks.
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
    capacityLock.readLock().lock();
    try {
      currentCapacity = capacity;
      do {
        prevUsed = used.get();
        newUsed = prevUsed + size;
        if (newUsed > currentCapacity) {
          return false;
        }
      } while (!used.compareAndSet(prevUsed, newUsed));
    } finally {
      capacityLock.readLock().unlock();
    }

    stateUpdated(prevUsed, newUsed, currentCapacity, currentCapacity);
    return true;
  }

  private void stateUpdated(long prevUsed, long newUsed, long prevCapacity, long newCapacity) {
    if (newUsed > prevUsed || newCapacity < prevCapacity) {
      // rising transition: used increased or capacity decreased
      float occupancy = (newUsed * 1.0f) / newCapacity;
      ThresholdWatermarks currentWatermarks = watermarks;
      if (Float.compare(occupancy, currentWatermarks.minUnfiredThreshold) >= 0) {
        checkRisingThresholds(occupancy, newUsed, newCapacity);
      }
    } else if (newUsed < prevUsed || newCapacity > prevCapacity) {
      // falling transition: used decreased or capacity increased
      float occupancy = (newUsed * 1.0f) / newCapacity;
      ThresholdWatermarks currentWatermarks = watermarks;
      if (Float.compare(occupancy, currentWatermarks.maxFiredThreshold) < 0) {
        checkFallingThresholds(occupancy, newUsed, newCapacity);
      }
    }

    monitor.sample(newCapacity - newUsed, newUsed);
  }

  private void checkRisingThresholds(float occupancy, long used, long capacity) {
    List<OffHeapUsageListener> listenersToFire = null;
    synchronized (listenerLock) {
      for (OffHeapUsageListener offHeapUsageListener : listenerMap.values()) {
        if (!offHeapUsageListener.isFired() && (Float.compare(offHeapUsageListener.getThreshold(), occupancy) <= 0)) {
          offHeapUsageListener.setFiringStatus(true);
          if (listenersToFire == null) {
            listenersToFire = new ArrayList<>();
          }
          listenersToFire.add(offHeapUsageListener);
        }
      }
      if (listenersToFire != null) {
        publishWatermarks();
      }
    }
    if (listenersToFire != null) {
      OffHeapUsageEvent offHeapUsageEvent = new OffHeapUsageEventImpl(used, capacity - used, capacity, OffHeapUsageEventType.RISING);
      RuntimeException failure = null;
      for (OffHeapUsageListener offHeapUsageListener : listenersToFire) {
        if (Float.compare(offHeapUsageListener.getThreshold(), 0.9f) == 0) {
          LOGGER.warn(MESSAGE_PROPERTIES.getProperty(OFFHEAP_WARN_KEY), identifier, (used * 100L) / capacity);
        } else if (Float.compare(offHeapUsageListener.getThreshold(), 0.75f) == 0) {
          LOGGER.info(MESSAGE_PROPERTIES.getProperty(OFFHEAP_INFO_KEY), identifier, (used * 100L) / capacity);
        }
        try {
          offHeapUsageListener.getConsumer().accept(offHeapUsageEvent);
        } catch (RuntimeException e) {
          if (failure == null) {
            failure = e;
          }
        }
      }
      if (failure != null) {
        throw failure;
      }
    }
  }

  private void checkFallingThresholds(float occupancy, long used, long capacity) {
    List<OffHeapUsageListener> listenersToFire = null;
    synchronized (listenerLock) {
      for (OffHeapUsageListener offHeapUsageListener : listenerMap.values()) {
        if (offHeapUsageListener.isFired() && (Float.compare(offHeapUsageListener.getThreshold(), occupancy) > 0)) {
          offHeapUsageListener.setFiringStatus(false);
          if (listenersToFire == null) {
            listenersToFire = new ArrayList<>();
          }
          listenersToFire.add(offHeapUsageListener);
        }
      }
      if (listenersToFire != null) {
        publishWatermarks();
      }
    }
    if (listenersToFire != null) {
      OffHeapUsageEvent offHeapUsageEvent = new OffHeapUsageEventImpl(used, capacity - used, capacity, OffHeapUsageEventType.FALLING);
      RuntimeException failure = null;
      for (OffHeapUsageListener offHeapUsageListener : listenersToFire) {
        if (Float.compare(offHeapUsageListener.getThreshold(), 0.75f) == 0) {
          LOGGER.info(MESSAGE_PROPERTIES.getProperty(OFFHEAP_INFO_KEY), identifier, (used * 100L) / capacity);
        }
        try {
          offHeapUsageListener.getConsumer().accept(offHeapUsageEvent);
        } catch (RuntimeException e) {
          if (failure == null) {
            failure = e;
          }
        }
      }
      if (failure != null) {
        throw failure;
      }
    }
  }

  private void publishWatermarks() {
    float minUnfired = Float.POSITIVE_INFINITY;
    float maxFired = Float.NEGATIVE_INFINITY;
    for (OffHeapUsageListener offHeapUsageListener : listenerMap.values()) {
      if (offHeapUsageListener.isFired()) {
        if (Float.compare(offHeapUsageListener.getThreshold(), maxFired) > 0) {
          maxFired = offHeapUsageListener.getThreshold();
        }
      } else if (Float.compare(offHeapUsageListener.getThreshold(), minUnfired) < 0) {
        minUnfired = offHeapUsageListener.getThreshold();
      }
    }
    watermarks = new ThresholdWatermarks(minUnfired, maxFired);
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

    long prevUsed;
    long newUsed;
    long currentCapacity;
    capacityLock.readLock().lock();
    try {
      currentCapacity = capacity;
      do {
        prevUsed = used.get();
        newUsed = prevUsed - Math.min(size, prevUsed);
      } while (!used.compareAndSet(prevUsed, newUsed));
    } finally {
      capacityLock.readLock().unlock();
    }

    stateUpdated(prevUsed, newUsed, currentCapacity, currentCapacity);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long available() {
    capacityLock.readLock().lock();
    try {
      return capacity - used.get();
    } finally {
      capacityLock.readLock().unlock();
    }
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

    long previousCapacity;
    long currentUsed;
    capacityLock.writeLock().lock();
    try {
      previousCapacity = capacity;
      currentUsed = used.get();
      if (currentUsed > size) {
        // shrinking below current usage is refused, exactly as the previous
        // single-state implementation refused it (withCapacity().isOverflowed())
        return false;
      }
      capacity = size;
    } finally {
      capacityLock.writeLock().unlock();
    }

    onCapacityChanged.onCapacityChanged(this, previousCapacity, size);
    stateUpdated(currentUsed, currentUsed, previousCapacity, size);
    return true;
  }

  @Override
  public void addUsageListener(UUID listenerUUID, float threshold, Consumer<OffHeapUsageEvent> consumer) {
    OffHeapUsageListener offHeapUsageListener = new OffHeapUsageListener(threshold, consumer);
    OffHeapUsageEvent immediateEvent = null;
    synchronized (listenerLock) {
      // widen the unfired watermark before the listener becomes visible to
      // concurrent scans, so a crossing that races this call cannot be skipped
      ThresholdWatermarks currentWatermarks = watermarks;
      if (Float.compare(threshold, currentWatermarks.minUnfiredThreshold) < 0) {
        watermarks = new ThresholdWatermarks(threshold, currentWatermarks.maxFiredThreshold);
      }
      listenerMap.put(listenerUUID, offHeapUsageListener);
      // check for rising event if current usage already is above threshold.
      long currentUsed = used.get();
      long currentCapacity = capacity;
      float occupancy = (currentUsed * 1.0f) / currentCapacity;
      if ((Float.compare(offHeapUsageListener.getThreshold(), occupancy) <= 0)) {
        offHeapUsageListener.setFiringStatus(true);
        immediateEvent = new OffHeapUsageEventImpl(currentUsed, currentCapacity - currentUsed, currentCapacity, OffHeapUsageEventType.RISING);
        publishWatermarks();
      }
    }
    if (immediateEvent != null) {
      offHeapUsageListener.getConsumer().accept(immediateEvent);
    }
  }

  @Override
  public void removeUsageListener(UUID listenerUUID) throws IllegalArgumentException {
    synchronized (listenerLock) {
      if (listenerMap.remove(listenerUUID) == null) {
        throw new IllegalArgumentException("Unknown listener: " + listenerUUID);
      }
      publishWatermarks();
    }
  }

  private static final class ThresholdWatermarks {
    static final ThresholdWatermarks NONE = new ThresholdWatermarks(Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY);

    private final float minUnfiredThreshold;
    private final float maxFiredThreshold;

    ThresholdWatermarks(float minUnfiredThreshold, float maxFiredThreshold) {
      this.minUnfiredThreshold = minUnfiredThreshold;
      this.maxFiredThreshold = maxFiredThreshold;
    }
  }
}