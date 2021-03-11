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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Ignore;

/**
 * @author Mathieu Carbou
 */
public class PathTest {

  @Test
  public void test_new_path_mapping() throws JsonProcessingException {
    ObjectMapper objectMapper = new ObjectMapper().registerModule(new TerracottaJsonModule());

    assertThat(objectMapper.writeValueAsString(new Foo()), is(equalTo("{\"path\":null}")));
    assertThat(objectMapper.writeValueAsString(new Foo().setPath(Paths.get(""))), is(equalTo("{\"path\":[\"\"]}")));
    assertThat(objectMapper.writeValueAsString(new Foo().setPath(Paths.get("foo"))), is(equalTo("{\"path\":[\"foo\"]}")));
    assertThat(objectMapper.writeValueAsString(new Foo().setPath(Paths.get("foo", "bar"))), is(equalTo("{\"path\":[\"foo\",\"bar\"]}")));

    assertThat(objectMapper.readValue("{\"path\":null}", Foo.class), is(equalTo(new Foo())));
    assertThat(objectMapper.readValue("{\"path\":[\"\"]}", Foo.class), is(equalTo(new Foo().setPath(Paths.get("")))));
    assertThat(objectMapper.readValue("{\"path\":[\"foo\"]}", Foo.class), is(equalTo(new Foo().setPath(Paths.get("foo")))));
    assertThat(objectMapper.readValue("{\"path\":[\"foo\",\"bar\"]}", Foo.class), is(equalTo(new Foo().setPath(Paths.get("foo", "bar")))));
  }

  @Test
  public void test_new_path_mapping_backward_compatible() throws JsonProcessingException {
    ObjectMapper objectMapper = new ObjectMapper().registerModule(new TerracottaJsonModule());

    assertThat(objectMapper.readValue("{\"path\":null}", Foo.class), is(equalTo(new Foo())));
    assertThat(objectMapper.readValue("{\"path\":\"\"}", Foo.class), is(equalTo(new Foo().setPath(Paths.get("")))));
    assertThat(objectMapper.readValue("{\"path\":\"foo\"}", Foo.class), is(equalTo(new Foo().setPath(Paths.get("foo")))));
    assertThat(objectMapper.readValue("{\"path\":\"foo/bar\"}", Foo.class), is(equalTo(new Foo().setPath(Paths.get("foo", "bar")))));
    if (isWindows()) {
      assertThat(objectMapper.readValue("{\"path\":\"foo\\\\bar\"}", Foo.class), is(equalTo(new Foo().setPath(Paths.get("foo", "bar")))));
    } else {
      assertThat(objectMapper.readValue("{\"path\":\"foo\\\\bar\"}", Foo.class), is(equalTo(new Foo().setPath(Paths.get("foo\\bar")))));
    }
  }

  @Test
  @SuppressWarnings("deprecation")
  public void test_buggy_path_mapping_v1() throws JsonProcessingException {
    ObjectMapper objectMapper = new ObjectMapper().registerModule(new TerracottaJsonModuleV1());

    assertThat(objectMapper.writeValueAsString(new Foo()), is(equalTo("{\"path\":null}")));
    assertThat(objectMapper.writeValueAsString(new Foo().setPath(Paths.get(""))), is(equalTo("{\"path\":\"\"}")));
    assertThat(objectMapper.writeValueAsString(new Foo().setPath(Paths.get("foo"))), is(equalTo("{\"path\":\"foo\"}")));
    if (isWindows()) {
      assertThat(objectMapper.writeValueAsString(new Foo().setPath(Paths.get("foo", "bar"))), is(equalTo("{\"path\":\"foo\\\\bar\"}")));
    } else {
      assertThat(objectMapper.writeValueAsString(new Foo().setPath(Paths.get("foo", "bar"))), is(equalTo("{\"path\":\"foo/bar\"}")));
    }

    assertThat(objectMapper.readValue("{\"path\":null}", Foo.class), is(equalTo(new Foo())));
    assertThat(objectMapper.readValue("{\"path\":\"\"}", Foo.class), is(equalTo(new Foo().setPath(Paths.get("")))));
    assertThat(objectMapper.readValue("{\"path\":\"foo\"}", Foo.class), is(equalTo(new Foo().setPath(Paths.get("foo")))));
    assertThat(objectMapper.readValue("{\"path\":\"foo/bar\"}", Foo.class), is(equalTo(new Foo().setPath(Paths.get("foo", "bar")))));
    if (isWindows()) {
      assertThat(objectMapper.readValue("{\"path\":\"foo\\\\bar\"}", Foo.class), is(equalTo(new Foo().setPath(Paths.get("foo", "bar")))));
    } else {
      // ⚠️ The V1 way of mapping the Path was not able to keep the correct segments
      // ⚠️ when a windows path was serialized and then deserialized on linux
      // ⚠️ The result should be: Paths.get("foo", "bar")
      assertThat(objectMapper.readValue("{\"path\":\"foo\\\\bar\"}", Foo.class), is(equalTo(new Foo().setPath(Paths.get("foo\\bar")))));
    }
  }

  private static boolean isWindows() {
    return System.getProperty("os.name").toLowerCase().startsWith("windows");
  }

  public static class Foo {
    private Path path;

    public Path getPath() {
      return path;
    }

    public Foo setPath(Path path) {
      this.path = path;
      return this;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof Foo)) return false;
      Foo foo = (Foo) o;
      return Objects.equals(getPath(), foo.getPath());
    }

    @Override
    public int hashCode() {
      return Objects.hash(getPath());
    }

    @Override
    public String toString() {
      if (path == null) {
        return "null";
      }
      List<String> segments = new ArrayList<>(path.getNameCount());
      for (Path p : path) {
        segments.add(p.toString());
      }
      return segments.toString();
    }
  }
}
