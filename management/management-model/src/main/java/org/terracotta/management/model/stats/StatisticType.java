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
package org.terracotta.management.model.stats;

import com.tc.classloader.CommonComponent;

/**
 * @author Mathieu Carbou
 */
@CommonComponent
public enum StatisticType {

  COUNTER,
  RATE,
  RATIO,
  GAUGE,
  TABLE;

  public static StatisticType convert(org.terracotta.statistics.StatisticType type) {
    switch (type) {
      case RATE: return RATE;
      case GAUGE: return GAUGE;
      case TABLE: return TABLE;
      case COUNTER: return COUNTER;
      case RATIO: return RATIO;
      default: throw new IllegalArgumentException("Un supported statistics type");
    }
  }

  public static org.terracotta.statistics.StatisticType convert(StatisticType type) {
    switch (type) {
      case RATE: return org.terracotta.statistics.StatisticType.RATE;
      case GAUGE: return org.terracotta.statistics.StatisticType.GAUGE;
      case TABLE: return org.terracotta.statistics.StatisticType.TABLE;
      case COUNTER: return org.terracotta.statistics.StatisticType.COUNTER;
      case RATIO: return org.terracotta.statistics.StatisticType.RATIO;
      default: throw new IllegalArgumentException("Un supported statistics type");
    }
  }

}
