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
package org.terracotta.dynamic_config.cli.converter;

import org.junit.Test;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.terracotta.inet.HostPort.create;

/**
 * @author Mathieu Carbou
 */
public class ShapeConverterTest {

  ShapeConverter converter = new ShapeConverter();

  @Test
  public void convert_hostports() {
    assertThat(converter.convert("bar"), is(equalTo(entry(set(create("bar", 9410)), null))));
    assertThat(converter.convert("bar:123"), is(equalTo(entry(set(create("bar", 123)), null))));
    assertThat(converter.convert("bar|baz"), is(equalTo(entry(set(create("bar", 9410), create("baz", 9410)), null))));
    assertThat(converter.convert("bar:123|baz"), is(equalTo(entry(set(create("bar", 123), create("baz", 9410)), null))));
    assertThat(converter.convert("bar|baz:456"), is(equalTo(entry(set(create("bar", 9410), create("baz", 456)), null))));
  }

  @Test
  public void convert_name_hostports() {
    assertThat(converter.convert("foo/bar"), is(equalTo(entry(set(create("bar", 9410)), "foo"))));
    assertThat(converter.convert("foo/bar:123"), is(equalTo(entry(set(create("bar", 123)), "foo"))));
    assertThat(converter.convert("foo/bar|baz"), is(equalTo(entry(set(create("bar", 9410), create("baz", 9410)), "foo"))));
    assertThat(converter.convert("foo/bar:123|baz"), is(equalTo(entry(set(create("bar", 123), create("baz", 9410)), "foo"))));
    assertThat(converter.convert("foo/bar|baz:456"), is(equalTo(entry(set(create("bar", 9410), create("baz", 456)), "foo"))));
  }

  @SafeVarargs
  @SuppressWarnings("varargs")
  private static <E> Set<E> set(E... els) {
    return new HashSet<>(Arrays.asList(els));
  }

  private static <K, V> Map.Entry<K, V> entry(K k1, V v1) {
    return new AbstractMap.SimpleEntry<>(k1, v1);
  }
}