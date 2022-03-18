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
package org.terracotta.common.struct;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.Test;
import org.terracotta.common.struct.json.StructJsonModule;

import java.util.EnumSet;
import java.util.Objects;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;
import static org.terracotta.common.struct.MemoryUnit.B;
import static org.terracotta.common.struct.MemoryUnit.GB;
import static org.terracotta.common.struct.TimeUnit.HOURS;
import static org.terracotta.common.struct.TimeUnit.MINUTES;
import static org.terracotta.common.struct.TimeUnit.SECONDS;
import static org.terracotta.testing.ExceptionMatcher.throwing;

/**
 * @author Mathieu Carbou
 */
public class MeasureTest {

  private final ObjectMapper json = new ObjectMapper()
      .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
      .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
      .registerModule(new StructJsonModule());

  @Test
  public void test_of() {
    assertThat(() -> Measure.of(1, null), is(throwing(instanceOf(NullPointerException.class))));
    assertThat(
        () -> Measure.of(-1, GB),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(containsString("cannot be negative"))));
  }

  @Test
  public void test_getQuantity() {
    assertThat(Measure.of(1, GB).getQuantity(), is(equalTo(1L)));
  }

  @Test
  public void test_getUnit() {
    assertThat(Measure.of(1, GB).getUnit(), is(equalTo(GB)));
  }

  @Test
  public void test_equals() {
    assertThat(Measure.of(1, GB), is(equalTo(Measure.of(1, GB))));
  }

  @Test
  public void test_hashCode() {
    assertThat(Measure.of(1, GB).hashCode(), is(equalTo(Measure.of(1, GB).hashCode())));
  }

  @Test
  public void test_toString() {
    assertThat(Measure.of(1, SECONDS).toString(), is(equalTo("1s")));
    assertThat(Measure.of(1, GB).toString(), is(equalTo("1GB")));
  }

  @Test
  public void test_parse() {
    assertThat(() -> Measure.parse(null, TimeUnit.class), is(throwing(instanceOf(NullPointerException.class))));
    assertThat(() -> Measure.parse("", null), is(throwing(instanceOf(NullPointerException.class))));

    assertThat(
        () -> Measure.parse("", TimeUnit.class, SECONDS, EnumSet.of(MINUTES, HOURS)),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Default unit 's' is not in the list of valid units [m, h].")))));

    assertThat(
        () -> Measure.parse("", TimeUnit.class),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Invalid measure. <quantity><unit> are missing.")))));

    assertThat(
        () -> Measure.parse("s", TimeUnit.class),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Invalid measure: 's'. <quantity> is missing. Measure should be specified in <quantity><unit> format.")))));

    assertThat(
        () -> Measure.parse("1", TimeUnit.class),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Invalid measure: '1'. <unit> is missing. Measure should be specified in <quantity><unit> format.")))));

    assertThat(
        () -> Measure.parse("1s", TimeUnit.class, null, EnumSet.of(MINUTES, HOURS)),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Invalid measure: '1s'. <unit> must be one of [m, h].")))));

    assertThat(
        () -> Measure.parse("-1s", TimeUnit.class, null, EnumSet.of(MINUTES, HOURS)),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(containsString("cannot be negative"))));

    assertThat(Measure.parse("1", TimeUnit.class, SECONDS).toString(), is(equalTo("1s")));
    assertThat(Measure.parse("1s", TimeUnit.class, null).toString(), is(equalTo("1s")));
    assertThat(Measure.parse("1s", TimeUnit.class).toString(), is(equalTo("1s")));
    assertThat(Measure.parse(Long.MAX_VALUE + "1s", TimeUnit.class).toString(), is(equalTo("92233720368547758071s")));
  }

  @Test
  public void test_to() {
    Measure<TimeUnit> measure = Measure.of(120, SECONDS).to(MINUTES);
    assertThat(measure.getQuantity(), is(equalTo(2L)));
    assertThat(measure.getUnit(), is(equalTo(MINUTES)));
  }

  @Test
  public void test_zero() {
    assertThat(Measure.zero(TimeUnit.class).getQuantity(), is(equalTo(0L)));
    assertThat(Measure.zero(TimeUnit.class).getUnit(), is(equalTo(SECONDS)));

    assertThat(Measure.zero(MemoryUnit.class).getQuantity(), is(equalTo(0L)));
    assertThat(Measure.zero(MemoryUnit.class).getUnit(), is(equalTo(B)));
  }

  @Test
  public void test_compareTo() {
    assertThat(Measure.of(60, SECONDS).compareTo(Measure.of(1, MINUTES)), is(equalTo(0)));
    assertThat(Measure.of(60, SECONDS).compareTo(Measure.of(2, MINUTES)), is(lessThan(0)));
    assertThat(Measure.of(120, SECONDS).compareTo(Measure.of(1, MINUTES)), is(greaterThan(0)));
  }

  @Test
  public void test_json() throws JsonProcessingException {
    Config config = new Config();
    String json = this.json.writeValueAsString(config);
    assertThat(json, is(equalTo("{\"leaseTime\":{\"quantity\":3,\"unit\":\"SECONDS\",\"type\":\"TIME\"},\"offheap\":{\"quantity\":1,\"unit\":\"GB\",\"type\":\"MEMORY\"}}")));
    assertThat(this.json.readValue(json, Config.class), is(equalTo(config)));
  }

  public static class Config {
    Measure<MemoryUnit> offheap = Measure.of(1, GB);
    Measure<TimeUnit> leaseTime = Measure.of(3, SECONDS);

    public Measure<MemoryUnit> getOffheap() {
      return offheap;
    }

    public void setOffheap(Measure<MemoryUnit> offheap) {
      this.offheap = offheap;
    }

    public Measure<TimeUnit> getLeaseTime() {
      return leaseTime;
    }

    public void setLeaseTime(Measure<TimeUnit> leaseTime) {
      this.leaseTime = leaseTime;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Config config = (Config) o;
      return Objects.equals(offheap, config.offheap) &&
          Objects.equals(leaseTime, config.leaseTime);
    }

    @Override
    public int hashCode() {
      return Objects.hash(offheap, leaseTime);
    }
  }

}
