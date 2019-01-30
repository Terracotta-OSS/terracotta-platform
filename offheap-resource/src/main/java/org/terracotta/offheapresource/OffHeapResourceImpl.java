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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.offheapresource.management.OffHeapResourceBinding;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

/**
 * An implementation of {@link OffHeapResource}.
 */
class OffHeapResourceImpl implements OffHeapResource {

  private static final Logger LOGGER = LoggerFactory.getLogger(OffHeapResourceImpl.class);

  private static final String MESSAGE_PROPERTIES_RESOURCE_NAME = "/offheap-message.properties";
  private static final String OFFHEAP_INFO_KEY = "offheap.info";
  private static final String OFFHEAP_WARN_KEY = "offheap.warn";
  private static final String DEFAULT_MESSAGE = "Offheap allocation for resource \"{}\" reached {}%, you may run out of memory if allocation continues.";
  private static final Properties MESSAGE_PROPERTIES;

  static {
    Properties defaults = new Properties();
    defaults.setProperty(OFFHEAP_INFO_KEY, DEFAULT_MESSAGE);
    defaults.setProperty(OFFHEAP_WARN_KEY, DEFAULT_MESSAGE);
    MESSAGE_PROPERTIES = new Properties(defaults);
    boolean loaded = false;
    try {
      InputStream resource = OffHeapResourceImpl.class.getResourceAsStream(MESSAGE_PROPERTIES_RESOURCE_NAME);
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
  private final BiConsumer<OffHeapResourceImpl, ThresholdChange> onReservationThresholdReached;
  private final OffHeapResourceBinding managementBinding;
  private final AtomicInteger threshold = new AtomicInteger();

  /**
   * Creates a resource of the given initial size.
   *
   *
   * @param identifier
   * @param size size of the resource
   * @throws IllegalArgumentException if the size is negative
   */
  OffHeapResourceImpl(String identifier, long size, BiConsumer<OffHeapResourceImpl, ThresholdChange> onReservationThresholdReached) throws IllegalArgumentException {
    this.onReservationThresholdReached = onReservationThresholdReached;
    this.managementBinding = new OffHeapResourceBinding(identifier, this);
    if (size < 0) {
      throw new IllegalArgumentException("Resource size cannot be negative");
    }

    this.state = new AtomicReference<>(new OffHeapResourceState(size));
    this.identifier = identifier;
  }

  OffHeapResourceImpl(String identifier, long size) throws IllegalArgumentException {
    this(identifier, size, (r, p) -> {});
  }

  public OffHeapResourceBinding getManagementBinding() {
    return managementBinding;
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

      if (newState.getUsed() > newState.getCapacity()) {
        return false;
      }

      if (state.compareAndSet(currentState, newState)) {
        stateUpdated(newState);
        return true;
      }
    }
  }

  private void stateUpdated(OffHeapResourceState newState) {
    long capacity = newState.getCapacity();
    long used = newState.getUsed();

    long percentOccupied = (used * 100L) / capacity;
    int newT, curT = threshold.get();
    if (percentOccupied >= 90L) {
      newT = 90;
    } else if (percentOccupied >= 75L) {
      newT = 75;
    } else {
      newT = 0;
    }
    if (threshold.compareAndSet(curT, newT)) {
      if (newT > curT) {
        // increase from 0->75 or 75->90
        if (newT == 90) {
          LOGGER.warn(MESSAGE_PROPERTIES.getProperty(OFFHEAP_WARN_KEY), identifier, percentOccupied);
        } else {
          LOGGER.info(MESSAGE_PROPERTIES.getProperty(OFFHEAP_INFO_KEY), identifier, percentOccupied);
        }
        onReservationThresholdReached.accept(this, new ThresholdChange(curT, newT));
      } else if (newT < curT) {
        // decrease from 90->75 or 75->0
        if (newT == 75) {
          LOGGER.info(MESSAGE_PROPERTIES.getProperty(OFFHEAP_INFO_KEY), identifier, percentOccupied);
        }
        onReservationThresholdReached.accept(this, new ThresholdChange(curT, newT));
      }
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
    }

    while (true) {
      OffHeapResourceState currentState = state.get();
      OffHeapResourceState newState = currentState.release(size);

      if (state.compareAndSet(currentState, newState)) {
        stateUpdated(newState);
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

      if (newState.getUsed() > newState.getCapacity()) {
        return false;
      }

      if (state.compareAndSet(currentState, newState)) {
        stateUpdated(newState);
        return true;
      }
    }
  }
  
  static class ThresholdChange {
    final int old;
    final int now;

    ThresholdChange(int old, int now) {
      this.old = old;
      this.now = now;
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
