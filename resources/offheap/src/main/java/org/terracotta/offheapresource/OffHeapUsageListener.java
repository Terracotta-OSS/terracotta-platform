/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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

import java.util.function.Consumer;

public class OffHeapUsageListener {
  private final float threshold;
  private final Consumer<OffHeapUsageEvent> consumer;
  private volatile boolean isFired = false;

  public OffHeapUsageListener(float threshold, Consumer<OffHeapUsageEvent> consumer) {
    this.threshold = threshold;
    this.consumer = consumer;
  }

  public float getThreshold() {
    return threshold;
  }

  public Consumer<OffHeapUsageEvent> getConsumer() {
    return consumer;
  }

  public boolean isFired() {
    return isFired;
  }

  public void setFiringStatus(boolean status) {
    isFired = status;
  }
}
