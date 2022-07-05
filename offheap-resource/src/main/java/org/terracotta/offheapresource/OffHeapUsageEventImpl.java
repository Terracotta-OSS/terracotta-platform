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

public class OffHeapUsageEventImpl implements OffHeapUsageEvent {
  private final long used;
  private final long available;
  private final long total;
  private final OffHeapUsageEventType usageEventType;

  public OffHeapUsageEventImpl(long used, long available, long total, OffHeapUsageEventType usageEventType) {
    this.used = used;
    this.total = total;
    this.available = available;
    this.usageEventType = usageEventType;
  }

  @Override
  public long getUsed() {
    return used;
  }

  @Override
  public long getTotal() {
    return total;
  }

  @Override
  public long getAvailable() {
    return available;
  }

  @Override
  public float getOccupancy() {
    return (used * 1.0f) / total;
  }

  @Override
  public OffHeapUsageEventType getEventType() {
    return usageEventType;
  }
}
