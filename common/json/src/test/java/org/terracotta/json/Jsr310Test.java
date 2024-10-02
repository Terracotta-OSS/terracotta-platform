/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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

import java.time.Duration;
import java.time.Instant;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class Jsr310Test {
  @Test
  public void test_instant() {
    final Json mapper = new DefaultJsonFactory().create();
    final long now = 1687969053056L;
    final Instant instant = Instant.ofEpochMilli(now);
    final String json = "\"2023-06-28T16:17:33.056Z\"";
    assertThat(mapper.parse(json, Instant.class), is(equalTo(instant)));
    assertThat(mapper.toString(instant), is(equalTo(json)));
  }

  @Test
  public void test_duration() {
    final Json mapper = new DefaultJsonFactory().create();
    final Duration o = Duration.ofMillis(1);
    final String json = "\"PT0.001S\"";
    assertThat(mapper.parse(json, Duration.class), is(equalTo(o)));
    assertThat(mapper.toString(o), is(equalTo(json)));
  }
}