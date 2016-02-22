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

import org.terracotta.management.Objects;
import org.terracotta.management.capabilities.context.CapabilityContext;
import org.terracotta.management.capabilities.descriptors.Descriptor;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * @author Ludovic Orban
 * @author Mathieu Carbou
 */
public final class StatisticsCapability implements Capability, Serializable {

  private final String name;
  private final Properties properties;
  private final Collection<Descriptor> descriptors;
  private final CapabilityContext capabilityContext;

  public StatisticsCapability(String name, Properties properties, CapabilityContext capabilityContext, Descriptor... descriptors) {
    this(name, properties, Arrays.asList(descriptors), capabilityContext);
  }

  public StatisticsCapability(String name, Properties properties, Collection<Descriptor> descriptors, CapabilityContext capabilityContext) {
    this.name = Objects.requireNonNull(name);
    this.properties = Objects.requireNonNull(properties);
    this.descriptors = Objects.requireNonNull(descriptors);
    this.capabilityContext = Objects.requireNonNull(capabilityContext);
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    StatisticsCapability that = (StatisticsCapability) o;

    if (!name.equals(that.name)) return false;
    if (!properties.equals(that.properties)) return false;
    if (!descriptors.equals(that.descriptors)) return false;
    return capabilityContext.equals(that.capabilityContext);

  }

  @Override
  public int hashCode() {
    int result = name.hashCode();
    result = 31 * result + properties.hashCode();
    result = 31 * result + descriptors.hashCode();
    result = 31 * result + capabilityContext.hashCode();
    return result;
  }

  public static class Properties implements Serializable {
    private final long averageWindowDuration;
    private final TimeUnit averageWindowUnit;
    private final int historySize;
    private final long historyInterval;
    private final TimeUnit historyIntervalUnit;
    private final long timeToDisable;
    private final TimeUnit timeToDisableUnit;

    public Properties(long averageWindowDuration, TimeUnit averageWindowUnit, int historySize, long historyInterval, TimeUnit historyIntervalUnit, long timeToDisable, TimeUnit timeToDisableUnit) {
      this.averageWindowDuration = averageWindowDuration;
      this.averageWindowUnit = Objects.requireNonNull(averageWindowUnit);
      this.historySize = historySize;
      this.historyInterval = historyInterval;
      this.historyIntervalUnit = Objects.requireNonNull(historyIntervalUnit);
      this.timeToDisable = timeToDisable;
      this.timeToDisableUnit = Objects.requireNonNull(timeToDisableUnit);
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

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      Properties that = (Properties) o;

      if (averageWindowDuration != that.averageWindowDuration) return false;
      if (historySize != that.historySize) return false;
      if (historyInterval != that.historyInterval) return false;
      if (timeToDisable != that.timeToDisable) return false;
      if (averageWindowUnit != that.averageWindowUnit) return false;
      if (historyIntervalUnit != that.historyIntervalUnit) return false;
      return timeToDisableUnit == that.timeToDisableUnit;

    }

    @Override
    public int hashCode() {
      int result = (int) (averageWindowDuration ^ (averageWindowDuration >>> 32));
      result = 31 * result + averageWindowUnit.hashCode();
      result = 31 * result + historySize;
      result = 31 * result + (int) (historyInterval ^ (historyInterval >>> 32));
      result = 31 * result + historyIntervalUnit.hashCode();
      result = 31 * result + (int) (timeToDisable ^ (timeToDisable >>> 32));
      result = 31 * result + timeToDisableUnit.hashCode();
      return result;
    }
  }

}
