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
package org.terracotta.management.stats;

import org.terracotta.management.stats.history.AverageHistory;
import org.terracotta.management.stats.history.CounterHistory;
import org.terracotta.management.stats.history.DurationHistory;
import org.terracotta.management.stats.history.RateHistory;
import org.terracotta.management.stats.history.RatioHistory;
import org.terracotta.management.stats.history.SizeHistory;
import org.terracotta.management.stats.primitive.Counter;
import org.terracotta.management.stats.primitive.Duration;
import org.terracotta.management.stats.primitive.Rate;
import org.terracotta.management.stats.primitive.Ratio;
import org.terracotta.management.stats.primitive.Size;

/**
 * @author Ludovic Orban
 */
public enum StatisticType {
  COUNTER(Counter.class),
  DURATION(Duration.class),
  RATE(Rate.class),
  RATIO(Ratio.class),
  SIZE(Size.class),

  COUNTER_HISTORY(CounterHistory.class),
  DURATION_HISTORY(DurationHistory.class),
  RATE_HISTORY(RateHistory.class),
  RATIO_HISTORY(RatioHistory.class),
  SIZE_HISTORY(SizeHistory .class),
  AVERAGE_HISTORY(AverageHistory.class),

  ;
  private final Class<?> clazz;

  StatisticType(Class<?> clazz) {
    this.clazz = clazz;
  }

  public Class<?> getClazz() {
    return clazz;
  }

  public String getTypeName() {
    return clazz.getSimpleName();
  }

}
