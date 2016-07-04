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

import org.terracotta.management.model.stats.history.AverageHistory;
import org.terracotta.management.model.stats.history.CounterHistory;
import org.terracotta.management.model.stats.history.DurationHistory;
import org.terracotta.management.model.stats.history.RateHistory;
import org.terracotta.management.model.stats.history.RatioHistory;
import org.terracotta.management.model.stats.history.SizeHistory;
import org.terracotta.management.model.stats.primitive.Average;
import org.terracotta.management.model.stats.primitive.Counter;
import org.terracotta.management.model.stats.primitive.Duration;
import org.terracotta.management.model.stats.primitive.Rate;
import org.terracotta.management.model.stats.primitive.Ratio;
import org.terracotta.management.model.stats.primitive.Size;

/**
 * @author Ludovic Orban
 */
public enum StatisticType {
  COUNTER(Counter.class),
  DURATION(Duration.class),
  RATE(Rate.class),
  RATIO(Ratio.class),
  SIZE(Size.class),
  AVERAGE(Average.class),

  COUNTER_HISTORY(CounterHistory.class),
  DURATION_HISTORY(DurationHistory.class),
  RATE_HISTORY(RateHistory.class),
  RATIO_HISTORY(RatioHistory.class),
  SIZE_HISTORY(SizeHistory.class),
  AVERAGE_HISTORY(AverageHistory.class),;
  private final Class<? extends Statistic<?, ?>> clazz;

  StatisticType(Class<? extends Statistic<?, ?>> clazz) {
    this.clazz = clazz;
  }

  public Class<? extends Statistic<?, ?>> getClazz() {
    return clazz;
  }

  public String getTypeName() {
    return clazz.getSimpleName();
  }

  public static StatisticType fromClass(Class<? extends Statistic<?, ?>> clazz) {
    for (StatisticType type : values()) {
      if (type.clazz == clazz) {
        return type;
      }
    }
    throw new IllegalArgumentException("Statistic type not found: " + clazz);
  }

}
