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
package org.terracotta.management.stats.jackson;

import org.terracotta.management.context.Context;
import org.terracotta.management.capabilities.ActionsCapability;
import org.terracotta.management.capabilities.Capability;
import org.terracotta.management.capabilities.StatisticsCapability;
import org.terracotta.management.capabilities.context.CapabilityContext;
import org.terracotta.management.capabilities.descriptors.CallDescriptor;
import org.terracotta.management.capabilities.descriptors.Descriptor;
import org.terracotta.management.capabilities.descriptors.StatisticDescriptorCategory;
import org.terracotta.management.capabilities.descriptors.StatisticDescriptor;
import org.terracotta.management.context.ContextContainer;
import org.terracotta.management.stats.Category;
import org.terracotta.management.stats.Sample;
import org.terracotta.management.stats.Statistic;
import org.terracotta.management.stats.jackson.mixins.context.ContextContainerMixIn;
import org.terracotta.management.stats.jackson.mixins.context.ContextMixIn;
import org.terracotta.management.stats.jackson.mixins.capabilities.ActionsCapabilityMixIn;
import org.terracotta.management.stats.jackson.mixins.capabilities.CapabilityMixIn;
import org.terracotta.management.stats.jackson.mixins.capabilities.StatisticsCapabilityMixIn;
import org.terracotta.management.stats.jackson.mixins.capabilities.context.CapabilityContextMixIn;
import org.terracotta.management.stats.jackson.mixins.capabilities.descriptors.CallDescriptorMixIn;
import org.terracotta.management.stats.jackson.mixins.capabilities.descriptors.StatisticDescriptorCategoryMixIn;
import org.terracotta.management.stats.jackson.mixins.capabilities.descriptors.DescriptorMixIn;
import org.terracotta.management.stats.jackson.mixins.capabilities.descriptors.StatisticDescriptorMixIn;
import org.terracotta.management.stats.jackson.mixins.stats.CategoryMixIn;
import org.terracotta.management.stats.jackson.mixins.stats.primitive.CounterMixIn;
import org.terracotta.management.stats.jackson.mixins.stats.primitive.DurationMixIn;
import org.terracotta.management.stats.jackson.mixins.stats.primitive.MeasurableSettingMixIn;
import org.terracotta.management.stats.jackson.mixins.stats.primitive.RateMixIn;
import org.terracotta.management.stats.jackson.mixins.stats.primitive.RatioMixIn;
import org.terracotta.management.stats.jackson.mixins.stats.SampleMixIn;
import org.terracotta.management.stats.jackson.mixins.stats.sampled.SampledCounterMixIn;
import org.terracotta.management.stats.jackson.mixins.stats.sampled.SampledDurationMixIn;
import org.terracotta.management.stats.jackson.mixins.stats.sampled.SampledRateMixIn;
import org.terracotta.management.stats.jackson.mixins.stats.sampled.SampledRatioMixIn;
import org.terracotta.management.stats.jackson.mixins.stats.sampled.SampledSizeMixIn;
import org.terracotta.management.stats.jackson.mixins.stats.primitive.SettingMixIn;
import org.terracotta.management.stats.jackson.mixins.stats.primitive.SizeMixIn;
import org.terracotta.management.stats.jackson.mixins.stats.StatisticMixIn;
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Ludovic Orban
 */
public class MixIns {

  private static final Map<Class<?>, Class<?>> MIXINS;

  static {
    Map<Class<?>, Class<?>> mixins = new HashMap<Class<?>, Class<?>>();

    // misc stats
    mixins.put(Statistic.class, StatisticMixIn.class);
    mixins.put(Sample.class, SampleMixIn.class);
    mixins.put(Category.class, CategoryMixIn.class);

    //primitive stats
    mixins.put(Setting.class, SettingMixIn.class);
    mixins.put(MeasurableSetting.class, MeasurableSettingMixIn.class);
    mixins.put(Counter.class, CounterMixIn.class);
    mixins.put(Rate.class, RateMixIn.class);
    mixins.put(Ratio.class, RatioMixIn.class);
    mixins.put(Duration.class, DurationMixIn.class);
    mixins.put(Size.class, SizeMixIn.class);

    //sample stats
    mixins.put(SampledCounter.class, SampledCounterMixIn.class);
    mixins.put(SampledRate.class, SampledRateMixIn.class);
    mixins.put(SampledRatio.class, SampledRatioMixIn.class);
    mixins.put(SampledDuration.class, SampledDurationMixIn.class);
    mixins.put(SampledSize.class, SampledSizeMixIn.class);

    //capabilities
    mixins.put(Capability.class, CapabilityMixIn.class);
    mixins.put(ActionsCapability.class, ActionsCapabilityMixIn.class);
    mixins.put(StatisticsCapability.class, StatisticsCapabilityMixIn.class);
    mixins.put(StatisticsCapability.Properties.class, StatisticsCapabilityMixIn.Properties.class);
    mixins.put(CapabilityContext.class, CapabilityContextMixIn.class);
    mixins.put(CapabilityContext.Attribute.class, CapabilityContextMixIn.Attribute.class);

    //descriptors
    mixins.put(Descriptor.class, DescriptorMixIn.class);
    mixins.put(CallDescriptor.class, CallDescriptorMixIn.class);
    mixins.put(CallDescriptor.Parameter.class, CallDescriptorMixIn.Parameter.class);
    mixins.put(StatisticDescriptor.class, StatisticDescriptorMixIn.class);
    mixins.put(StatisticDescriptorCategory.class, StatisticDescriptorCategoryMixIn.class);

    //context
    mixins.put(Context.class, ContextMixIn.class);
    mixins.put(ContextContainer.class, ContextContainerMixIn.class);

    MIXINS = Collections.unmodifiableMap(mixins);
  }

  private MixIns() {
  }

  public static Map<Class<?>, Class<?>> mixins() {
    return MIXINS;
  }

}
