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
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class EnumTest {
  @Test
  public void test() {
    final Json mapper = new DefaultJsonFactory().create();
    final Foo foo = new Foo().setUnit(TimeUnit.SECONDS);
    final String json = "{\"unit\":\"SECONDS\"}";
    assertThat(mapper.toString(foo), is(equalTo(json)));
    assertThat(mapper.parse(json, Foo.class), is(equalTo(foo)));
  }

  public static class Foo {
    private TimeUnit unit;

    public Foo setUnit(TimeUnit unit) {
      this.unit = unit;
      return this;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Foo foo = (Foo) o;
      return unit == foo.unit;
    }

    @Override
    public int hashCode() {
      return Objects.hash(unit);
    }
  }
}