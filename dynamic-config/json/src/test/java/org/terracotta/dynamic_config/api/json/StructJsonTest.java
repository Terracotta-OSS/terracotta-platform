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
package org.terracotta.dynamic_config.api.json;

import org.junit.Test;
import org.terracotta.common.struct.Measure;
import org.terracotta.common.struct.MemoryUnit;
import org.terracotta.common.struct.TimeUnit;
import org.terracotta.common.struct.Version;
import org.terracotta.json.DefaultJsonFactory;
import org.terracotta.json.Json;

import java.util.Objects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.terracotta.common.struct.MemoryUnit.GB;
import static org.terracotta.common.struct.TimeUnit.SECONDS;

/**
 * @author Mathieu Carbou
 */
public class StructJsonTest {

  private final Json json = new DefaultJsonFactory().withModule(new DynamicConfigJsonModule()).create();

  @Test
  public void test_version() {
    for (String v : new String[]{"5.8.6-pre7", "0.0.9", "3.9.5-internal5", "10.7.0.3", "10.7.0.3-SNAPSHOT", "10.7.0.3-foo-SNAPSHOT", "10.7.0.3.foo-SNAPSHOT"}) {
      assertThat(Version.valueOf(v).toString(), is(equalTo(v)));
      String json = this.json.toString(Version.valueOf(v));
      assertThat(json, is(equalTo("\"" + v + "\"")));
      assertThat(this.json.parse(json, Version.class), is(equalTo(Version.valueOf(v))));
    }
  }

  @Test
  public void test_measure() {
    Config config = new Config();
    String json = this.json.toString(config);
    assertThat(json, is(equalTo("{\"leaseTime\":{\"quantity\":3,\"type\":\"TIME\",\"unit\":\"SECONDS\"},\"offheap\":{\"quantity\":1,\"type\":\"MEMORY\",\"unit\":\"GB\"}}")));
    assertThat(this.json.parse(json, Config.class), is(equalTo(config)));
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
