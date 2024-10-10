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
import org.terracotta.json.gson.GsonConfig;
import org.terracotta.json.gson.GsonModule;
import org.terracotta.json.util.DirectedGraph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class ModuleTest {
  static final Collection<String> CALLS = new ArrayList<>();

  @Test
  public void test_dependency() {
    CALLS.clear();
    new DefaultJsonFactory().withModule(new B()).create();
    assertThat(CALLS, is(equalTo(asList("A", "B"))));
  }

  @Test
  public void test_transitive_dependency() {
    CALLS.clear();
    new DefaultJsonFactory().withModule(new D()).create();
    assertThat(CALLS, is(equalTo(asList("A", "B", "D"))));
  }

  @Test
  public void test_override() {
    CALLS.clear();
    new DefaultJsonFactory().withModule(new B()).withModule(new C()).create();
    assertThat(CALLS, is(equalTo(asList("C", "A", "B"))));
  }

  @Test
  public void test_ignore_override() {
    CALLS.clear();
    new DefaultJsonFactory().withModule(new C()).create();
    assertThat(CALLS, is(equalTo(singletonList("C"))));
  }

  @Test
  public void test_dft() {
    // A->B means:
    // - A is a dependency of B
    // - or A overrides B

    // A->B->C
    // A->D->C
    // D->E
    // F->A

    {
      final DirectedGraph<String> graph = new DirectedGraph<>();
      Stream.of("A", "B", "C", "D", "E", "F", "G").forEach(v -> graph.addEdge("R", v));

      graph.addEdge("A", "B");
      graph.addEdge("A", "D");
      graph.addEdge("B", "C");
      graph.addEdge("D", "C");
      graph.addEdge("D", "E");
      graph.addEdge("F", "A");

      final List<String> list = graph.depthFirstTraversal("R").collect(toList());
      Collections.reverse(list);
      assertThat(list, equalTo(Arrays.asList("R", "G", "F", "A", "D", "E", "B", "C")));
    }
  }

  static abstract class TestModule implements GsonModule {
    @Override
    public void configure(GsonConfig config) {
      CALLS.add(getClass().getSimpleName());
    }
  }

  static class A extends TestModule {}

  @Json.Module.Dependencies(A.class)
  static class B extends TestModule {}

  @Json.Module.Overrides(A.class)
  static class C extends TestModule {}

  @Json.Module.Dependencies(B.class)
  static class D extends TestModule {}
}