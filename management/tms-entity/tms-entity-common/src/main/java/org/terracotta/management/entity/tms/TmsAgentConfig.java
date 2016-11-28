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
package org.terracotta.management.entity.tms;

import org.terracotta.management.registry.collect.StatisticConfiguration;

import java.io.Serializable;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @author Mathieu Carbou
 */
public final class TmsAgentConfig implements Serializable {

  private static final long serialVersionUID = 1;

  // name must be hardcoded because it reference a class name in client package and is used on server-side
  public static final String ENTITY_TYPE = "org.terracotta.management.entity.tms.client.TmsAgentEntity";

  private int maximumUnreadMessages = 1024 * 1024;
  private StatisticConfiguration statisticConfiguration = new StatisticConfiguration(
      60, SECONDS,
      100, 1, SECONDS,
      30, SECONDS
  );

  public int getMaximumUnreadMessages() {
    return maximumUnreadMessages;
  }

  public TmsAgentConfig setMaximumUnreadMessages(int maximumUnreadMessages) {
    this.maximumUnreadMessages = maximumUnreadMessages;
    return this;
  }

  public StatisticConfiguration getStatisticConfiguration() {
    return statisticConfiguration;
  }

  public TmsAgentConfig setStatisticConfiguration(StatisticConfiguration statisticConfiguration) {
    this.statisticConfiguration = statisticConfiguration;
    return this;
  }

  @Override
  public String toString() {
    return "TmsAgentConfig{" + "maximumUnreadMessages=" + maximumUnreadMessages + '}';
  }

}
