
/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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
package org.terracotta.json;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class SerializationTest {

  @Test
  public void test_any_subtypes() {
    final Json mapper = new DefaultJsonFactory().create();
    Map<String, Statistic<Long>> statistics = Collections.singletonMap("Puts", new Statistic<>("COUNTER", Arrays.asList(new Sample<Long>(1, 1L), new Sample<Long>(2, 2L))));
    assertThat(mapper.toString(statistics), is(equalTo("{\"Puts\":{\"samples\":[{\"timestamp\":1,\"value\":1},{\"timestamp\":2,\"value\":2}],\"type\":\"COUNTER\"}}")));
  }

  static class Statistic<T> {
    String type;
    List<Sample<T>> samples;

    public Statistic(String type, List<Sample<T>> samples) {
      this.type = type;
      this.samples = samples;
    }
  }

  static class Sample<T> {
    long timestamp;
    T value;

    public Sample(long timestamp, T value) {
      this.timestamp = timestamp;
      this.value = value;
    }
  }
}