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
package org.terracotta.management.capabilities;

import org.terracotta.management.capabilities.context.CapabilityContext;
import org.terracotta.management.capabilities.descriptors.Descriptor;

import java.io.Serializable;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * @author Ludovic Orban
 */
public final class StatisticsCapability implements Capability, Serializable {

  private final String name;
  private final Properties properties;
  private final Collection<Descriptor> descriptors;
  private final CapabilityContext capabilityContext;

  public StatisticsCapability(String name, Properties properties, Collection<Descriptor> descriptors, CapabilityContext capabilityContext) {
    this.name = name;
    this.properties = properties;
    this.descriptors = descriptors;
    this.capabilityContext = capabilityContext;
  }

  public Properties getProperties() {
    return properties;
  }

  public Collection<Descriptor> getDescriptors() {
    return descriptors;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public CapabilityContext getCapabilityContext() {
    return capabilityContext;
  }

  public static class Properties {
    private final long averageWindowDuration;
    private final TimeUnit averageWindowUnit;
    private final int historySize;
    private final long historyInterval;
    private final TimeUnit historyIntervalUnit;
    private final long timeToDisable;
    private final TimeUnit timeToDisableUnit;

    public Properties(long averageWindowDuration, TimeUnit averageWindowUnit, int historySize, long historyInterval, TimeUnit historyIntervalUnit, long timeToDisable, TimeUnit timeToDisableUnit) {
      this.averageWindowDuration = averageWindowDuration;
      this.averageWindowUnit = averageWindowUnit;
      this.historySize = historySize;
      this.historyInterval = historyInterval;
      this.historyIntervalUnit = historyIntervalUnit;
      this.timeToDisable = timeToDisable;
      this.timeToDisableUnit = timeToDisableUnit;
    }

    public long getAverageWindowDuration() {
      return averageWindowDuration;
    }

    public TimeUnit getAverageWindowUnit() {
      return averageWindowUnit;
    }

    public int getHistorySize() {
      return historySize;
    }

    public long getHistoryInterval() {
      return historyInterval;
    }

    public TimeUnit getHistoryIntervalUnit() {
      return historyIntervalUnit;
    }

    public long getTimeToDisable() {
      return timeToDisable;
    }

    public TimeUnit getTimeToDisableUnit() {
      return timeToDisableUnit;
    }
  }

}
