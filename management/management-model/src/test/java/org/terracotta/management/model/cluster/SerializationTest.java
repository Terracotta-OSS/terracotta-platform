/*
 * Copyright Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.terracotta.management.model.cluster;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.terracotta.management.model.call.ContextualReturn;
import org.terracotta.management.model.capabilities.DefaultCapability;
import org.terracotta.management.model.capabilities.StatisticsCapability;
import org.terracotta.management.model.capabilities.context.CapabilityContext;
import org.terracotta.management.model.capabilities.descriptors.CallDescriptor;
import org.terracotta.management.model.capabilities.descriptors.StatisticDescriptor;
import org.terracotta.management.model.capabilities.descriptors.StatisticDescriptorCategory;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.context.ContextContainer;
import org.terracotta.management.model.notification.ContextualNotification;
import org.terracotta.management.model.stats.ContextualStatistics;
import org.terracotta.management.model.stats.MemoryUnit;
import org.terracotta.management.model.stats.NumberUnit;
import org.terracotta.management.model.stats.Sample;
import org.terracotta.management.model.stats.Statistic;
import org.terracotta.management.model.stats.StatisticType;
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

/**
 * @author Mathieu Carbou
 */
@RunWith(JUnit4.class)
public class SerializationTest {

  private Context context = Context.create("cacheManagerName", "my-cm-1");

  @Test
  public void test_contextualReturn() throws Exception {
    ContextualReturn<Integer> contextualReturn = ContextualReturn.of("capability", context, "method", 1);
    assertEquals(contextualReturn, copy(contextualReturn));
    assertEquals(contextualReturn.hashCode(), copy(contextualReturn).hashCode());
  }

  @Test
  public void test_contextcontainer() throws Exception {
    ContextContainer contextContainer = new ContextContainer("cmName", "cm1", new ContextContainer("cacheName", "cache1"));
    assertEquals(contextContainer, copy(contextContainer));
    assertEquals(contextContainer.hashCode(), copy(contextContainer).hashCode());
  }

  @Test
  public void test_capability() throws Exception {
    DefaultCapability actionsCapability = new DefaultCapability(
        "capability",
        new CapabilityContext(new CapabilityContext.Attribute("cache", true)),
        new CallDescriptor("clear", Void.TYPE.getName(), new CallDescriptor.Parameter("cache", String.class.getName())));
    assertEquals(actionsCapability, copy(actionsCapability));
    assertEquals(actionsCapability.hashCode(), copy(actionsCapability).hashCode());

    StatisticsCapability statisticsCapability = new StatisticsCapability(
        "capability",
        new StatisticsCapability.Properties(1, TimeUnit.HOURS, 100, 1, TimeUnit.SECONDS, 5, TimeUnit.SECONDS),
        new CapabilityContext(new CapabilityContext.Attribute("cache", true)),
        new StatisticDescriptorCategory("category", new StatisticDescriptor("stat", StatisticType.AVERAGE)),
        new StatisticDescriptor("stat2", StatisticType.AVERAGE_HISTORY));
    assertEquals(statisticsCapability, copy(statisticsCapability));
    assertEquals(statisticsCapability.hashCode(), copy(statisticsCapability).hashCode());
  }

  @Test
  public void test_contextualNotif() throws Exception {
    ContextualNotification notif = new ContextualNotification(context, "TYPE", Context.create("key", "val"));
    assertEquals(notif, copy(notif));
    assertEquals(notif.hashCode(), copy(notif).hashCode());
  }

  @Test
  public void test_contextualStats() throws Exception {
    ContextualStatistics statistics = new ContextualStatistics("capability", context, new HashMap<String, Statistic<?, ?>>() {{
      put("stat1", new AverageHistory(singletonList(new Sample<Double>(1, 1.0)), TimeUnit.MILLISECONDS));
      put("stat2", new CounterHistory(singletonList(new Sample<Long>(1, 1L)), NumberUnit.COUNT));
      put("stat3", new DurationHistory(singletonList(new Sample<Long>(1, 1L)), TimeUnit.DAYS));
      put("stat4", new RateHistory(singletonList(new Sample<Double>(1, 1.0)), TimeUnit.DAYS));
      put("stat5", new RatioHistory(singletonList(new Sample<Double>(1, 1.0)), NumberUnit.RATIO));
      put("stat6", new SizeHistory(singletonList(new Sample<Long>(1, 1L)), MemoryUnit.B));

      put("stat11", new Average(1.0, TimeUnit.MILLISECONDS));
      put("stat12", new Counter(1L, NumberUnit.COUNT));
      put("stat13", new Duration(1L, TimeUnit.DAYS));
      put("stat14", new Rate(1.0, TimeUnit.DAYS));
      put("stat15", new Ratio(1.0, NumberUnit.RATIO));
      put("stat16", new Size(1L, MemoryUnit.B));
    }});
    assertEquals(statistics, copy(statistics));
    assertEquals(statistics.hashCode(), copy(statistics).hashCode());
  }

  @SuppressWarnings("unchecked")
  private static <T> T copy(T o) throws IOException, ClassNotFoundException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(baos);
    oos.writeObject(o);
    oos.close();
    ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
    return (T) in.readObject();
  }
}
