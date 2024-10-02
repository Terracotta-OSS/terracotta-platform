/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2025
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
import java.util.concurrent.atomic.AtomicReference;
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

  private final AtomicReference<OffHeapResourceState> state;
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

    this.state = new AtomicReference<>(new OffHeapResourceState(size));
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

    while (true) {
      OffHeapResourceState currentState = state.get();
      OffHeapResourceState newState = currentState.reserve(size);

      if (newState.isOverflowed()) {
        return false;
      }

      if (state.compareAndSet(currentState, newState)) {
        stateUpdated(currentState, newState);
        return true;
      }
    }
  }

  private void stateUpdated(OffHeapResourceState prevState, OffHeapResourceState newState) {
    long capacity = newState.getCapacity();
    long used = newState.getUsed();
    long prevUsed = prevState.getUsed();
    long prevCapacity = prevState.getCapacity();

    if (used > prevUsed || capacity < prevCapacity) {
      // check for rising event.
      float occupancy = (used * 1.0f) / capacity;
      OffHeapUsageEvent offHeapUsageEvent = null;
      for (OffHeapUsageListener offHeapUsageListener : listenerMap.values()) {
        if (!offHeapUsageListener.isFired() && (Float.compare(offHeapUsageListener.getThreshold(), occupancy) <= 0)) {
          if (offHeapUsageEvent == null) {
            offHeapUsageEvent = new OffHeapUsageEventImpl(used, newState.getRemaining(), capacity, OffHeapUsageEventType.RISING);
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
    } else if (used < prevUsed || capacity > prevCapacity) {
      // check for falling event.
      float occupancy = (used * 1.0f) / capacity;
      OffHeapUsageEvent offHeapUsageEvent = null;
      for (OffHeapUsageListener offHeapUsageListener : listenerMap.values()) {
        if (offHeapUsageListener.isFired() && (Float.compare(offHeapUsageListener.getThreshold(), occupancy) > 0)) {
          if (offHeapUsageEvent == null) {
            offHeapUsageEvent = new OffHeapUsageEventImpl(used, newState.getRemaining(), capacity, OffHeapUsageEventType.FALLING);
          }
          if (Float.compare(offHeapUsageListener.getThreshold(), 0.75f) == 0) {
            LOGGER.info(MESSAGE_PROPERTIES.getProperty(OFFHEAP_INFO_KEY), identifier, (used * 100L) / capacity);
          }
          offHeapUsageListener.getConsumer().accept(offHeapUsageEvent);
          offHeapUsageListener.setFiringStatus(false);
        }
      }
    }

    monitor.sample(capacity - used, used);
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

    while (true) {
      OffHeapResourceState currentState = state.get();
      OffHeapResourceState newState = currentState.release(size);

      if (state.compareAndSet(currentState, newState)) {
        stateUpdated(currentState, newState);
        return;
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long available() {
    return state.get().getRemaining();
  }

  @Override
  public long capacity() {
    return state.get().getCapacity();
  }

  @Override
  public boolean setCapacity(long size) throws IllegalArgumentException {
    if (size < 0) {
      throw new IllegalArgumentException("New capacity size cannot be negative");
    }

    while (true) {
      OffHeapResourceState currentState = state.get();
      OffHeapResourceState newState = currentState.withCapacity(size);

      if (newState.isOverflowed()) {
        return false;
      }

      if (state.compareAndSet(currentState, newState)) {
        onCapacityChanged.onCapacityChanged(this, currentState.getCapacity(), newState.getCapacity());
        stateUpdated(currentState, newState);
        return true;
      }
    }
  }

  @Override
  public void addUsageListener(UUID listenerUUID, float threshold, Consumer<OffHeapUsageEvent> consumer) {
    OffHeapUsageListener offHeapUsageListener = new OffHeapUsageListener(threshold, consumer);
    listenerMap.put(listenerUUID, offHeapUsageListener);
    // check for rising event if current usage already is above threshold.
    OffHeapResourceState offHeapResourceState = state.get();
    long used = offHeapResourceState.used;
    long capacity = offHeapResourceState.capacity;
    float occupancy = (used * 1.0f) / capacity;
    if ((Float.compare(offHeapUsageListener.getThreshold(), occupancy) <= 0)) {
      OffHeapUsageEvent offHeapUsageEvent = new OffHeapUsageEventImpl(used, offHeapResourceState.getRemaining(), capacity, OffHeapUsageEventType.RISING);
      offHeapUsageListener.getConsumer().accept(offHeapUsageEvent);
      offHeapUsageListener.setFiringStatus(true);
    }
  }

  @Override
  public void removeUsageListener(UUID listenerUUID) throws IllegalArgumentException {
    if (listenerMap.remove(listenerUUID) == null) {
      throw new IllegalArgumentException("Unknown listener: " + listenerUUID);
    }
  }

  private static class OffHeapResourceState {
    private final long capacity;
    private final long used;

    public OffHeapResourceState(long capacity) {
      this.capacity = capacity;
      this.used = 0;
    }

    private OffHeapResourceState(long capacity, long used) {
      this.capacity = capacity;
      this.used = used;
    }

    public long getCapacity() {
      return capacity;
    }

    public long getUsed() {
      return used;
    }

    public long getRemaining() {
      return capacity - used;
    }

    public boolean isOverflowed() {
      return used > capacity;
    }

    public OffHeapResourceState reserve(long size) {
      return new OffHeapResourceState(capacity, used + size);
    }

    public OffHeapResourceState release(long size) {
      return new OffHeapResourceState(capacity, used - size);
    }

    public OffHeapResourceState withCapacity(long newCapacity) {
      return new OffHeapResourceState(newCapacity, used);
    }
  }
}
