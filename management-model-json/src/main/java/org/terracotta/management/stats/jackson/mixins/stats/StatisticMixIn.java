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
package org.terracotta.management.stats.jackson.mixins.stats;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.terracotta.management.stats.primitive.Counter;
import org.terracotta.management.stats.primitive.Duration;
import org.terracotta.management.stats.primitive.MeasurableSetting;
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
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "statisticType")
@JsonSubTypes({
    @JsonSubTypes.Type(value = Setting.class, name = "Setting"),
    @JsonSubTypes.Type(value = MeasurableSetting.class, name = "MeasurableSetting"),
    @JsonSubTypes.Type(value = Counter.class, name = "Counter"),
    @JsonSubTypes.Type(value = Rate.class, name = "Rate"),
    @JsonSubTypes.Type(value = Ratio.class, name = "Ratio"),
    @JsonSubTypes.Type(value = Duration.class, name = "Duration"),
    @JsonSubTypes.Type(value = Size.class, name = "Size"),

    @JsonSubTypes.Type(value = SampledCounter.class, name = "SampledCounter"),
    @JsonSubTypes.Type(value = SampledRate.class, name = "SampledRate"),
    @JsonSubTypes.Type(value = SampledRatio.class, name = "SampledRatio"),
    @JsonSubTypes.Type(value = SampledDuration.class, name = "SampledDuration"),
    @JsonSubTypes.Type(value = SampledSize.class, name = "SampledSize")
})
public abstract class StatisticMixIn {
}
