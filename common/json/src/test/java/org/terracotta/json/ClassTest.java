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
import org.terracotta.json.gson.GsonModule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

public class ClassTest {

  @Test
  public void test() {
    final Json mapper = new DefaultJsonFactory().withModule((GsonModule) builder -> {
      builder.allowClassLoading(List.class, HashMap.class);
      builder.allowClassLoading(Object.class, Void.class);
    }).create();

    Object[] oo = {
        List.class,
        HashMap.class,
        Object.class,
        Void.class,
        new Foo().setType(List.class),
        new Foo().setType(HashMap.class),
        new Foo().setType(Object.class),
        new Foo().setType(Void.class),
    };
    final String[] jsons = {
        "\"java.util.List\"",
        "\"java.util.HashMap\"",
        "\"java.lang.Object\"",
        "\"java.lang.Void\"",
        "{\"type\":\"java.util.List\"}",
        "{\"type\":\"java.util.HashMap\"}",
        "{\"type\":\"java.lang.Object\"}",
        "{\"type\":\"java.lang.Void\"}",
    };

    for (int i = 0; i < oo.length; i++) {
      assertThat(mapper.toString(oo[i]), is(equalTo(jsons[i])));
      assertThat(mapper.parse(jsons[i], oo[i].getClass()), is(equalTo(oo[i])));

      // in a map
      assertThat(mapper.toString(singletonMap("shape", oo[i])), is(equalTo("{\"shape\":" + jsons[i] + "}")));

      // Gson is not able to read a @type attribute when the destination type is object. It has to be wrapped.
      Map<String, Object> parsed = mapper.parseObject("{\"shape\":" + jsons[i] + "}");
      assertThat(parsed.get("shape"), either(is(instanceOf(String.class))).or(is(instanceOf(Map.class))));
    }

    oo = new Object[]{
        ArrayList.class,
        new Foo().setType(ArrayList.class),
    };

    for (Object o : oo) {
      try {
        mapper.toString(o);
        fail();
      } catch (Exception e) {
        assertThat(e.getMessage(), containsString("Attempted to serialize java.lang.Class: java.util.ArrayList. Forgot to register a type adapter"));
      }
    }
  }

  public static class Foo {
    private Class<?> type;

    public Foo setType(Class<?> type) {
      this.type = type;
      return this;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Foo foo = (Foo) o;
      return Objects.equals(type, foo.type);
    }

    @Override
    public int hashCode() {
      return Objects.hash(type);
    }
  }
}