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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.terracotta.dynamic_config.api.json.DynamicConfigModelJsonModule;
import org.terracotta.json.ObjectMapperFactory;

import java.util.Objects;
import java.util.Properties;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.terracotta.dynamic_config.api.model.Testing.newTestCluster;
import static org.terracotta.dynamic_config.api.model.Testing.newTestStripe;

/**
 * @author Mathieu Carbou
 */
public class RawPathTest {

  ObjectMapper om = new ObjectMapperFactory().withModule(new DynamicConfigModelJsonModule()).create();

  @Test
  public void test_new_path_mapping() throws JsonProcessingException {


    assertThat(om.writeValueAsString(new Foo()), is(equalTo("{}")));
    assertThat(om.writeValueAsString(new Foo().setPath(RawPath.valueOf(""))), is(equalTo("{\"path\":\"\"}")));
    assertThat(om.writeValueAsString(new Foo().setPath(RawPath.valueOf("foo"))), is(equalTo("{\"path\":\"foo\"}")));
    assertThat(om.writeValueAsString(new Foo().setPath(RawPath.valueOf("foo/bar"))), is(equalTo("{\"path\":\"foo/bar\"}")));
    assertThat(om.writeValueAsString(new Foo().setPath(RawPath.valueOf("foo\\bar"))), is(equalTo("{\"path\":\"foo\\\\bar\"}")));

    assertThat(om.readValue("{\"path\":null}", Foo.class), is(equalTo(new Foo())));
    assertThat(om.readValue("{\"path\":\"\"}", Foo.class), is(equalTo(new Foo().setPath(RawPath.valueOf("")))));
    assertThat(om.readValue("{\"path\":\"foo\"}", Foo.class), is(equalTo(new Foo().setPath(RawPath.valueOf("foo")))));

    // keeps both win and lin
    assertThat(om.readValue("{\"path\":\"foo/bar\"}", Foo.class), is(equalTo(new Foo().setPath(RawPath.valueOf("foo/bar")))));
    assertThat(om.readValue("{\"path\":\"foo\\\\bar\"}", Foo.class), is(equalTo(new Foo().setPath(RawPath.valueOf("foo\\bar")))));
  }

  @Test
  public void test_props_and_json() throws JsonProcessingException {
    Node node = Testing.newTestNode("foo", "localhost").setLogDir(RawPath.valueOf("a\\b"));
    Cluster cluster = newTestCluster("c", newTestStripe("s").addNode(node));

    Properties properties = cluster.toProperties(false, false, true);
    assertTrue(properties.containsKey("stripe.1.node.1.log-dir"));
    assertThat(properties.getProperty("stripe.1.node.1.log-dir"), is(equalTo("a\\b")));

    String expectedJson = "{\"stripes\":[{\"name\":\"s\",\"nodes\":[{\"hostname\":\"localhost\",\"logDir\":\"a\\\\b\",\"name\":\"foo\",\"uid\":\"jUhhu1kRQd-x6iNgpo9Xyw\"}],\"uid\":\"5Zv3uphiRLavoGZthy7JNg\"}],\"failoverPriority\":\"availability\",\"name\":\"c\",\"uid\":\"YLQguzhRSdS6y5M9vnA5mw\"}";
    assertThat(om.writeValueAsString(cluster), om.writeValueAsString(cluster), is(equalTo(expectedJson)));
    assertThat(om.readValue(expectedJson, Cluster.class), is(equalTo(cluster)));
    assertThat(om.readValue(expectedJson, Cluster.class).toProperties(false, false, true), is(equalTo(properties)));
  }

  public static class Foo {
    private RawPath path;

    public RawPath getPath() {
      return path;
    }

    public Foo setPath(RawPath path) {
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
      return String.valueOf(path);
    }
  }
}
