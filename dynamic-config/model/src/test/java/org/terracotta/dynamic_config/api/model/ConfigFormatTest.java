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
package org.terracotta.dynamic_config.api.model;

import org.junit.Test;

import java.nio.file.Paths;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Mathieu Carbou
 */
public class ConfigFormatTest {

  @Test
  public void testToString() {
    assertThat(Stream.of(ConfigFormat.values()).map(Objects::toString).sorted().collect(Collectors.joining(",")), is(equalTo("<unknown>,cfg,json,properties")));
  }

  @Test
  public void from() {
    assertThat(ConfigFormat.from(Paths.get("foo.properties")), is(equalTo(ConfigFormat.PROPERTIES)));
    assertThat(ConfigFormat.from(Paths.get("foo.cfg")), is(equalTo(ConfigFormat.CONFIG)));
    assertThat(ConfigFormat.from(Paths.get("foo.conf")), is(equalTo(ConfigFormat.CONFIG)));
    assertThat(ConfigFormat.from(Paths.get("foo.config")), is(equalTo(ConfigFormat.CONFIG)));
    assertThat(ConfigFormat.from(Paths.get("foo.json")), is(equalTo(ConfigFormat.JSON)));
    assertThat(ConfigFormat.from(Paths.get("foo")), is(equalTo(ConfigFormat.UNKNOWN)));
    assertThat(ConfigFormat.from(Paths.get("")), is(equalTo(ConfigFormat.UNKNOWN)));
    assertThat(ConfigFormat.from(Paths.get(".")), is(equalTo(ConfigFormat.UNKNOWN)));
    assertThat(ConfigFormat.from(Paths.get("/")), is(equalTo(ConfigFormat.UNKNOWN)));
  }

  @Test
  public void supported() {
    assertThat(String.join(",", ConfigFormat.supported()), is(equalTo("cfg,properties")));
  }
}