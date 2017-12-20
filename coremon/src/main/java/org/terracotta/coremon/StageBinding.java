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
package org.terracotta.coremon;

import org.terracotta.management.service.monitoring.registry.provider.AliasBinding;

public class StageBinding extends AliasBinding {

  private final int queueCount;
  private final int maxQueueSize;

  public StageBinding(String alias, JmxUtil jmxUtil) throws Exception {
    super(alias, jmxUtil);
    this.queueCount = (int) getValue().getStatistics().get(getAlias() + ".queueCount");
    this.maxQueueSize = (int) getValue().getStatistics().get(getAlias() + ".maxQueueSize");
  }

  @Override
  public JmxUtil getValue() {
    return (JmxUtil) super.getValue();
  }

  public int getQueueCount() {
    return queueCount;
  }

  public int getMaxQueueSize() {
    return maxQueueSize;
  }

  public int fetchCurrentQueueSize() {
    try {
      return (int) getValue().getStatistics().get(getAlias() + ".currentQueueSize");
    } catch (Exception e) {
      return -1;
    }
  }

  public long fetchTotalQueuedCount() {
    try {
      return (long) getValue().getStatistics().get(getAlias() + ".totalQueuedCount");
    } catch (Exception e) {
      return -1L;
    }
  }
}
