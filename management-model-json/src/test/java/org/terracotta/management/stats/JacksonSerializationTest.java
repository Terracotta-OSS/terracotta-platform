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

import org.hamcrest.CoreMatchers;
import org.junit.Test;
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

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Ludovic Orban
 */
public class JacksonSerializationTest {

  @Test
  public void testRead() throws Exception {
    Setting setting = Json.fromJson("{\"name\":\"CacheName\",\"value\":\"myCache\",\"statisticType\":\"Setting\"}");
    assertThat(setting.getName(), equalTo("CacheName"));
    assertThat(setting.getValue(), CoreMatchers.<Object>equalTo("myCache"));

//    Json.fromJSON("{\"name\":\"MaxCacheSize\",\"value\":4,\"unit\":\"GB\",\"type\":\"MeasurableSetting\"}");

    Counter counter = Json.fromJson("{\"name\":\"CacheHits\",\"value\":5000,\"statisticType\":\"Counter\"}");
    assertThat(counter.getName(), equalTo("CacheHits"));
    assertThat(counter.getValue(), equalTo(5000L));

    Rate rate = Json.fromJson("{\"name\":\"CacheHitRate\",\"value\":1250.1,\"unit\":\"SECONDS\",\"statisticType\":\"Rate\"}");
    assertThat(rate.getName(), equalTo("CacheHitRate"));
    assertThat(rate.getValue(), equalTo(1250.1));
    assertThat(rate.getUnit(), equalTo(TimeUnit.SECONDS));

    Ratio ratio = Json.fromJson("{\"name\":\"CacheHitRatio\",\"value\":0.95,\"statisticType\":\"Ratio\"}");
    assertThat(ratio.getName(), equalTo("CacheHitRatio"));
    assertThat(ratio.getValue(), equalTo(.95));

    Duration duration = Json.fromJson("{\"name\":\"CacheAverageGetTime\",\"value\":500,\"unit\":\"MICROSECONDS\",\"statisticType\":\"Duration\"}");
    assertThat(duration.getName(), equalTo("CacheAverageGetTime"));
    assertThat(duration.getValue(), equalTo(500L));
    assertThat(duration.getUnit(), equalTo(TimeUnit.MICROSECONDS));

    Size size = Json.fromJson("{\"name\":\"CacheSize\",\"value\":500,\"unit\":\"MB\",\"statisticType\":\"Size\"}");
    assertThat(size.getName(), equalTo("CacheSize"));
    assertThat(size.getValue(), equalTo(500L));
    assertThat(size.getUnit(), equalTo(MemoryUnit.MB));

    SampledCounter sampledCounter = Json.fromJson("{\"name\":\"CacheHitsSample\",\"value\":[{\"timestamp\":1000,\"value\":2000},{\"timestamp\":2000,\"value\":2200},{\"timestamp\":3000,\"value\":2300}],\"statisticType\":\"SampledCounter\"}");
    assertThat(sampledCounter.getName(), equalTo("CacheHitsSample"));
    assertThat(sampledCounter.getValue().size(), equalTo(3));
    assertThat(sampledCounter.getValue().get(0).getTimestamp(), equalTo(1000L));
    assertThat(sampledCounter.getValue().get(0).getValue(), equalTo(2000L));

    SampledRate sampledRate = Json.fromJson("{\"name\":\"CacheHitRateSample\",\"unit\":\"SECONDS\",\"value\":[{\"timestamp\":1000,\"value\":2000.2},{\"timestamp\":2000,\"value\":2200},{\"timestamp\":3000,\"value\":2300}],\"statisticType\":\"SampledRate\"}");
    assertThat(sampledRate.getName(), equalTo("CacheHitRateSample"));
    assertThat(sampledRate.getUnit(), equalTo(TimeUnit.SECONDS));
    assertThat(sampledRate.getValue().size(), equalTo(3));
    assertThat(sampledRate.getValue().get(0).getTimestamp(), equalTo(1000L));
    assertThat(sampledRate.getValue().get(0).getValue(), equalTo(2000.2));

    SampledRatio sampledRatio = Json.fromJson("{\"name\":\"CacheHitRatio\",\"value\":[{\"timestamp\":1000,\"value\":0.91},{\"timestamp\":2000,\"value\":0.92},{\"timestamp\":3000,\"value\":0.93}],\"statisticType\":\"SampledRatio\"}");
    assertThat(sampledRatio.getName(), equalTo("CacheHitRatio"));
    assertThat(sampledRatio.getValue().size(), equalTo(3));
    assertThat(sampledRatio.getValue().get(0).getTimestamp(), equalTo(1000L));
    assertThat(sampledRatio.getValue().get(0).getValue(), equalTo(.91));

    SampledDuration sampledDuration = Json.fromJson("{\"name\":\"CacheAverageGetTime\",\"unit\":\"MICROSECONDS\",\"value\":[{\"timestamp\":1000,\"value\":100},{\"timestamp\":2000,\"value\":150},{\"timestamp\":3000,\"value\":180}],\"statisticType\":\"SampledDuration\"}");
    assertThat(sampledDuration.getName(), equalTo("CacheAverageGetTime"));
    assertThat(sampledDuration.getUnit(), equalTo(TimeUnit.MICROSECONDS));
    assertThat(sampledDuration.getValue().size(), equalTo(3));
    assertThat(sampledDuration.getValue().get(0).getTimestamp(), equalTo(1000L));
    assertThat(sampledDuration.getValue().get(0).getValue(), equalTo(100L));

    SampledSize sampledSize = Json.fromJson("{\"name\":\"CacheSizeSample\",\"value\":[{\"timestamp\":1000,\"value\":2000},{\"timestamp\":2000,\"value\":2200},{\"timestamp\":3000,\"value\":2300}],\"unit\":\"MB\",\"statisticType\":\"SampledSize\"}");
    assertThat(sampledSize.getName(), equalTo("CacheSizeSample"));
    assertThat(sampledSize.getValue().size(), equalTo(3));
    assertThat(sampledSize.getValue().get(0).getTimestamp(), equalTo(1000L));
    assertThat(sampledSize.getValue().get(0).getValue(), equalTo(2000L));
  }

  @Test
  public void testWrite() throws Exception {
    String setting = Json.toJson(new Setting<String>("CacheName", "myCache"));
    assertThat(setting, equalTo("{\"statisticType\":\"Setting\",\"name\":\"CacheName\",\"value\":\"myCache\"}"));

    String measurableSetting = Json.toJson(new MeasurableSetting<Long, MemoryUnit>("MaxCacheSize", 4L, MemoryUnit.GB));
    assertThat(measurableSetting, equalTo("{\"statisticType\":\"MeasurableSetting\",\"name\":\"MaxCacheSize\",\"value\":4,\"unit\":\"GB\"}"));

    String counter = Json.toJson(new Counter("CacheHits", 5000L));
    assertThat(counter, equalTo("{\"statisticType\":\"Counter\",\"name\":\"CacheHits\",\"value\":5000}"));

    String rate = Json.toJson(new Rate("CacheHitRate", 1250.5, TimeUnit.SECONDS));
    assertThat(rate, equalTo("{\"statisticType\":\"Rate\",\"name\":\"CacheHitRate\",\"value\":1250.5,\"unit\":\"SECONDS\"}"));

    String ratio = Json.toJson(new Ratio("CacheHitRatio", .95));
    assertThat(ratio, equalTo("{\"statisticType\":\"Ratio\",\"name\":\"CacheHitRatio\",\"value\":0.95}"));

    String duration = Json.toJson(new Duration("CacheAverageGetTime", 500L, TimeUnit.MICROSECONDS));
    assertThat(duration, equalTo("{\"statisticType\":\"Duration\",\"name\":\"CacheAverageGetTime\",\"value\":500,\"unit\":\"MICROSECONDS\"}"));

    String size = Json.toJson(new Size("CacheSize", 500L, MemoryUnit.MB));
    assertThat(size, equalTo("{\"statisticType\":\"Size\",\"name\":\"CacheSize\",\"value\":500,\"unit\":\"MB\"}"));

    String sampledCounter = Json.toJson(new SampledCounter("CacheHitsSample", Arrays.asList(new Sample<Long>(1000L, 2000L), new Sample<Long>(2000L, 2200L), new Sample<Long>(3000L, 2300L))));
    assertThat(sampledCounter, equalTo("{\"statisticType\":\"SampledCounter\",\"name\":\"CacheHitsSample\",\"value\":[{\"timestamp\":1000,\"value\":2000},{\"timestamp\":2000,\"value\":2200},{\"timestamp\":3000,\"value\":2300}]}"));

    String sampledRate = Json.toJson(new SampledRate("CacheHitRateSample", Arrays.asList(new Sample<Double>(1000L, 2000.7), new Sample<Double>(2000L, 2200.6), new Sample<Double>(3000L, 2300.0)), TimeUnit.SECONDS));
    assertThat(sampledRate, equalTo("{\"statisticType\":\"SampledRate\",\"name\":\"CacheHitRateSample\",\"value\":[{\"timestamp\":1000,\"value\":2000.7},{\"timestamp\":2000,\"value\":2200.6},{\"timestamp\":3000,\"value\":2300.0}],\"unit\":\"SECONDS\"}"));

    String sampledRatio = Json.toJson(new SampledRatio("CacheHitRatio", Arrays.asList(new Sample<Double>(1000L, .91), new Sample<Double>(2000L, .92), new Sample<Double>(3000L, .93))));
    assertThat(sampledRatio, equalTo("{\"statisticType\":\"SampledRatio\",\"name\":\"CacheHitRatio\",\"value\":[{\"timestamp\":1000,\"value\":0.91},{\"timestamp\":2000,\"value\":0.92},{\"timestamp\":3000,\"value\":0.93}]}"));

    String sampledDuration = Json.toJson(new SampledDuration("CacheAverageGetTime", Arrays.asList(new Sample<Long>(1000L, 100L), new Sample<Long>(2000L, 150L), new Sample<Long>(3000L, 180L)), TimeUnit.MICROSECONDS));
    assertThat(sampledDuration, equalTo("{\"statisticType\":\"SampledDuration\",\"name\":\"CacheAverageGetTime\",\"value\":[{\"timestamp\":1000,\"value\":100},{\"timestamp\":2000,\"value\":150},{\"timestamp\":3000,\"value\":180}],\"unit\":\"MICROSECONDS\"}"));

    String sampledSize = Json.toJson(new SampledSize("CacheSizeSample", Arrays.asList(new Sample<Long>(1000L, 2000L), new Sample<Long>(2000L, 2200L), new Sample<Long>(3000L, 2300L)), MemoryUnit.MB));
    assertThat(sampledSize, equalTo("{\"statisticType\":\"SampledSize\",\"name\":\"CacheSizeSample\",\"unit\":\"MB\",\"value\":[{\"timestamp\":1000,\"value\":2000},{\"timestamp\":2000,\"value\":2200},{\"timestamp\":3000,\"value\":2300}]}"));
  }

  @Test
  public void testDeserializingSerializedStat() throws Exception {
    assertEqualityAfterSerializationAndDeserialization(new Setting<String>("CacheName", "myCache"));
//    assertEquality(new MeasurableSetting<Long, MemoryUnit>("MaxCacheSize", 4L, MemoryUnit.GB));
    assertEqualityAfterSerializationAndDeserialization(new Counter("CacheHits", 5000L));
    assertEqualityAfterSerializationAndDeserialization(new Rate("CacheHitRate", 1250.1, TimeUnit.SECONDS));
    assertEqualityAfterSerializationAndDeserialization(new Ratio("CacheHitRatio", .95));
    assertEqualityAfterSerializationAndDeserialization(new Duration("CacheAverageGetTime", 500L, TimeUnit.MICROSECONDS));
    assertEqualityAfterSerializationAndDeserialization(new Size("CacheSize", 500L, MemoryUnit.MB));
    assertEqualityAfterSerializationAndDeserialization(new SampledCounter("CacheHitsSample", Arrays.asList(new Sample<Long>(1000L, 2000L), new Sample<Long>(2000L, 2200L), new Sample<Long>(3000L, 2300L))));
    assertEqualityAfterSerializationAndDeserialization(new SampledRate("CacheHitRateSample", Arrays.asList(new Sample<Double>(1000L, 2000.1), new Sample<Double>(2000L, 2200.2), new Sample<Double>(3000L, 2300.3)), TimeUnit.SECONDS));
    assertEqualityAfterSerializationAndDeserialization(new SampledRatio("CacheHitRatio", Arrays.asList(new Sample<Double>(1000L, .91), new Sample<Double>(2000L, .92), new Sample<Double>(3000L, .93))));
    assertEqualityAfterSerializationAndDeserialization(new SampledDuration("CacheAverageGetTime", Arrays.asList(new Sample<Long>(1000L, 100L), new Sample<Long>(2000L, 150L), new Sample<Long>(3000L, 180L)), TimeUnit.MICROSECONDS));
    assertEqualityAfterSerializationAndDeserialization(new SampledSize("CacheSizeSample", Arrays.asList(new Sample<Long>(1000L, 2000L), new Sample<Long>(2000L, 2200L), new Sample<Long>(3000L, 2300L)), MemoryUnit.MB));
  }

  private void assertEqualityAfterSerializationAndDeserialization(Object o) throws IOException {
    assertThat(Json.fromJson(Json.toJson(o)), CoreMatchers.equalTo(o));
  }
}
