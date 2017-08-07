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
import java.util.concurrent.atomic.AtomicLong;
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

  private final AtomicLong remaining;
  private final long capacity;
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
    } else {
      this.capacity = size;
      this.remaining = new AtomicLong(size);
      this.identifier = identifier;
    }
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
    } else {
      for (long current = remaining.get(); current >= size; current = remaining.get()) {
        if (remaining.compareAndSet(current, current - size)) {
          remainingUpdated(current - size);
          return true;
        }
      }
      return false;
    }
  }

  private void remainingUpdated(long remaining) {
    long percentOccupied = (capacity - remaining) * 100 / capacity;
    int newT, curT = threshold.get();
    if (percentOccupied >= 90) {
      newT = 90;
    } else if (percentOccupied >= 75) {
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
    } else {
      long remaining = this.remaining.addAndGet(size);
      remainingUpdated(remaining);
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
  
  static class ThresholdChange {
    final int old;
    final int now;

    ThresholdChange(int old, int now) {
      this.old = old;
      this.now = now;
    }
  }
}
