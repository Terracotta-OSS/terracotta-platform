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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Mathieu Carbou
 */
public class OptionalTest {

  @Test
  public void test() {
    Json json = new DefaultJsonFactory().create();

    assertThat(json.toString(new Foo()), is(equalTo("{}")));
    assertThat(json.toString(new Foo().setPath(Optional.of(Paths.get("")))), is(equalTo("{\"path\":[\"\"]}")));
    assertThat(json.toString(new Foo().setPath(null)), is(equalTo("{}")));
    assertThat(json.toString(new Foo().setPath(Optional.ofNullable(null))), is(equalTo("{}")));

    assertThat(json.parse("{\"path\":null}", Foo.class), is(equalTo(new Foo())));
    assertThat(json.parse("{}", Foo.class), is(equalTo(new Foo())));
    assertThat(json.parse("{\"path\":[\"foo\"]}", Foo.class), is(equalTo(new Foo().setPath(Optional.of(Paths.get("foo"))))));
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public static class Foo {
    private Optional<Path> path;

    public Foo setPath(Optional<Path> path) {
      this.path = path;
      return this;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Foo foo = (Foo) o;
      return Objects.equals(path, foo.path);
    }

    @Override
    public int hashCode() {
      return Objects.hash(path);
    }
  }
}
