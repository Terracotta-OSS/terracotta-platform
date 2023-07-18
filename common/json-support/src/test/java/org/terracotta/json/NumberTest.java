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
package org.terracotta.json;

import org.junit.Test;

import java.util.Objects;

import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/*
Here are some Jackon examples:

import com.fasterxml.jackson.databind.ObjectMapper
ObjectMapper json = new ObjectMapper()
println json.readValue(json.writeValueAsString([n: Float.MAX_VALUE]), Map).n.class // Double
println json.readValue(json.writeValueAsString([n: Double.MAX_VALUE]), Map).n.class // Double
println json.readValue(json.writeValueAsString([n: 0.1f]), Map).n.class // Double
println json.readValue(json.writeValueAsString([n: 0.1d]), Map).n.class // Double
println json.readValue(json.writeValueAsString([n: Byte.MAX_VALUE]), Map).n.class // Integer
println json.readValue(json.writeValueAsString([n: Short.MAX_VALUE]), Map).n.class // Integer
println json.readValue(json.writeValueAsString([n: Integer.MAX_VALUE]), Map).n.class // Integer
println json.readValue(json.writeValueAsString([n: Long.MAX_VALUE]), Map).n.class // Long
println json.readValue(json.writeValueAsString([n: 1f]), Map).n.class // Double
println json.readValue(json.writeValueAsString([n: 1d]), Map).n.class // Double
*/
public class NumberTest {

  final Json mapper = new DefaultJsonFactory().create();

  @Test
  public void test_defaults() {
    assertThat(mapper.toString(singletonMap("value", 1)), is(equalTo("{\"value\":1}")));
    assertThat(mapper.toString(singletonMap("value", 1L)), is(equalTo("{\"value\":1}")));
    assertThat(mapper.toString(singletonMap("value", 1.0f)), is(equalTo("{\"value\":1.0}")));
    assertThat(mapper.toString(singletonMap("value", 1.0D)), is(equalTo("{\"value\":1.0}")));

    assertThat(mapper.parse("{\"value\":1}", N.class), is(equalTo(new N(1))));
    assertThat(mapper.parse("{\"value\":1.0}", N.class), is(equalTo(new N(1d))));
  }

  @Test
  public void test_special_values() {
    assertThat(mapper.toString(singletonMap("value", Float.NaN)), is(equalTo("{\"value\":\"NaN\"}")));
    assertThat(mapper.toString(singletonMap("value", Float.NEGATIVE_INFINITY)), is(equalTo("{\"value\":\"-Infinity\"}")));
    assertThat(mapper.toString(singletonMap("value", Float.POSITIVE_INFINITY)), is(equalTo("{\"value\":\"Infinity\"}")));
    assertThat(mapper.toString(singletonMap("value", Double.NaN)), is(equalTo("{\"value\":\"NaN\"}")));
    assertThat(mapper.toString(singletonMap("value", Double.NEGATIVE_INFINITY)), is(equalTo("{\"value\":\"-Infinity\"}")));
    assertThat(mapper.toString(singletonMap("value", Double.POSITIVE_INFINITY)), is(equalTo("{\"value\":\"Infinity\"}")));

    assertThat(mapper.parse("{\"value\":\"NaN\"}", N.class), is(equalTo(new N(Double.NaN))));
    assertThat(mapper.parse("{\"value\":\"Infinity\"}", N.class), is(equalTo(new N(Double.POSITIVE_INFINITY))));
    assertThat(mapper.parse("{\"value\":\"-Infinity\"}", N.class), is(equalTo(new N(Double.NEGATIVE_INFINITY))));

    assertThat(mapper.parse(mapper.toString(Float.NaN), Float.class), is(equalTo(Float.NaN)));
    assertThat(mapper.parse(mapper.toString(Double.NaN), Double.class), is(equalTo(Double.NaN)));
    assertThat(mapper.parse(mapper.toString(Float.NEGATIVE_INFINITY), Float.class), is(equalTo(Float.NEGATIVE_INFINITY)));
    assertThat(mapper.parse(mapper.toString(Double.NEGATIVE_INFINITY), Double.class), is(equalTo(Double.NEGATIVE_INFINITY)));
    assertThat(mapper.parse(mapper.toString(Float.POSITIVE_INFINITY), Float.class), is(equalTo(Float.POSITIVE_INFINITY)));
    assertThat(mapper.parse(mapper.toString(Double.POSITIVE_INFINITY), Double.class), is(equalTo(Double.POSITIVE_INFINITY)));
  }

  @Test
  public void test_ser_deser() {
    assertThat(mapper.parse(mapper.toString(Byte.MAX_VALUE), Byte.class), is(equalTo(Byte.MAX_VALUE)));
    assertThat(mapper.parse(mapper.toString(Integer.MAX_VALUE), Integer.class), is(equalTo(Integer.MAX_VALUE)));
    assertThat(mapper.parse(mapper.toString(Long.MAX_VALUE), Long.class), is(equalTo(Long.MAX_VALUE)));
    assertThat(mapper.parse(mapper.toString(Float.MAX_VALUE), Float.class), is(equalTo(Float.MAX_VALUE)));
    assertThat(mapper.parse(mapper.toString(Double.MAX_VALUE), Double.class), is(equalTo(Double.MAX_VALUE)));
  }

  static class N {
    Number value;

    // For Json
    private N() {
    }

    public N(Number value) {
      this.value = value;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      N n = (N) o;
      return Objects.equals(value, n.value);
    }

    @Override
    public int hashCode() {
      return Objects.hash(value);
    }
  }
}