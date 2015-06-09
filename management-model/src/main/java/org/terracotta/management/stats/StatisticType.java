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

import org.terracotta.management.stats.primitive.Counter;
import org.terracotta.management.stats.primitive.Duration;
import org.terracotta.management.stats.primitive.Rate;
import org.terracotta.management.stats.primitive.Ratio;
import org.terracotta.management.stats.primitive.Setting;
import org.terracotta.management.stats.primitive.Size;
import org.terracotta.management.stats.sampled.SampledCounter;
import org.terracotta.management.stats.sampled.SampledDuration;
import org.terracotta.management.stats.sampled.SampledRate;
import org.terracotta.management.stats.sampled.SampledRatio;
import org.terracotta.management.stats.sampled.SampledSize;

/**
 * @author Ludovic Orban
 */
public enum StatisticType {
  SETTING(Setting.class),
  COUNTER(Counter.class),
  DURATION(Duration.class),
  RATE(Rate.class),
  RATIO(Ratio.class),
  SIZE(Size.class),

  SAMPLED_COUNTER(SampledCounter.class),
  SAMPLED_DURATION(SampledDuration.class),
  SAMPLED_RATE(SampledRate.class),
  SAMPLED_RATIO(SampledRatio.class),
  SAMPLED_SIZE(SampledSize.class),

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
