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

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import org.junit.Test;
import org.terracotta.json.gson.GsonModule;
import org.terracotta.json.gson.UnsafeObjectTypeAdapterFactory;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

public class MixinTest {

  @Test
  public void test() {
    final Json mapper = new DefaultJsonFactory().withModule((GsonModule) builder -> {
      builder.registerSuperType(Shape.class).withSubtype(Square.class);
      builder.registerMixin(Bar.class, BarMixin.class);
      builder.registerMixin(Baz.class, BazMixin.class);
      builder.allowClassLoading(Square.class, TimeUnit.class);
    }).create();

    final Object[] oo = {
        new Square(3),
        new Bar<>().setShape(new Square(3)),
        new Baz<>().setBody(new Square(3)),
        new Baz<>().setBody(TimeUnit.MILLISECONDS),
    };
    final String[] jsons = {
        "{\"type\":\"Square\",\"x\":3}",
        "{\"shape\":{\"@class\":\"org.terracotta.json.MixinTest$Square\",\"type\":\"Square\",\"x\":3}}",
        "{\"body\":{\"@class\":\"org.terracotta.json.MixinTest$Square\",\"type\":\"Square\",\"x\":3}}",
        "{\"body\":[\"java.util.concurrent.TimeUnit\",\"MILLISECONDS\"]}"
    };

    for (int i = 0; i < oo.length; i++) {
      assertThat(String.valueOf(i), mapper.toString(oo[i]), is(equalTo(jsons[i])));
      // in a map
      assertThat(mapper.toString(singletonMap("shape", oo[i])), is(equalTo("{\"shape\":" + jsons[i] + "}")));
    }

    for (int i = 0; i < oo.length; i++) {
      assertThat(mapper.parse(jsons[i], oo[i].getClass()), is(equalTo(oo[i])));
      // Gson is not able to read a @type attribute when the destination type is object. It has to be wrapped.
      Map<String, Object> parsed = mapper.parseObject("{\"shape\":" + jsons[i] + "}");
      assertThat(parsed.get("shape"), is(instanceOf(Map.class)));
    }
  }

  @Test
  public void test_type_override() {
    final Json mapper = new DefaultJsonFactory().withModule((GsonModule) builder -> {
      builder.registerSuperType(Shape.class).withSubtype(Square.class);
      builder.registerMixin(Bar.class, BarMixin2.class);
      builder.registerMixin(Baz.class, BazMixin2.class);
    }).create();

    final Object[] oo = {
        new Bar<>().setShape(new Square(3)),
        new Baz<>().setBody(TimeUnit.MILLISECONDS),
    };
    final String[] jsons = {
        "{\"shape\":{\"type\":\"Square\",\"x\":3}}",
        "{\"body\":\"MILLISECONDS\"}"
    };

    for (int i = 0; i < oo.length; i++) {
      assertThat(mapper.toString(oo[i]), is(equalTo(jsons[i])));
      // in a map
      assertThat(mapper.toString(singletonMap("shape", oo[i])), is(equalTo("{\"shape\":" + jsons[i] + "}")));
    }

    for (int i = 0; i < oo.length; i++) {
      assertThat(mapper.parse(jsons[i], oo[i].getClass()), is(equalTo(oo[i])));
      // Gson is not able to read a @type attribute when the destination type is object. It has to be wrapped.
      Map<String, Object> parsed = mapper.parseObject("{\"shape\":" + jsons[i] + "}");
      assertThat(parsed.get("shape"), is(instanceOf(Map.class)));
    }
  }

  @Test
  public void test_exclusions() {
    // ser == true, deser == true
    Json mapper = new DefaultJsonFactory().withModule((GsonModule) builder -> builder.registerMixin(Fields.class, FieldsMixin1.class)).create();
    assertThat(mapper.toString(new Fields("a")), is(equalTo("{\"a\":\"a\"}")));
    assertThat(mapper.parse("{\"a\":\"a\"}", Fields.class), is(equalTo(new Fields("a"))));
    assertThat(mapper.parse("{}", Fields.class), is(equalTo(new Fields(null))));

    // ser == false, deser == false
    mapper = new DefaultJsonFactory().withModule((GsonModule) builder -> builder.registerMixin(Fields.class, FieldsMixin2.class)).create();
    assertThat(mapper.toString(new Fields("a")), is(equalTo("{}")));
    assertThat(mapper.parse("{\"a\":\"a\"}", Fields.class), is(equalTo(new Fields(null))));
    assertThat(mapper.parse("{}", Fields.class), is(equalTo(new Fields(null))));

    // ser == true, deser == false
    mapper = new DefaultJsonFactory().withModule((GsonModule) builder -> builder.registerMixin(Fields.class, FieldsMixin3.class)).create();
    assertThat(mapper.toString(new Fields("a")), is(equalTo("{\"a\":\"a\"}")));
    assertThat(mapper.parse("{\"a\":\"a\"}", Fields.class), is(equalTo(new Fields(null))));
    assertThat(mapper.parse("{}", Fields.class), is(equalTo(new Fields(null))));

    // ser == false, deser == true
    mapper = new DefaultJsonFactory().withModule((GsonModule) builder -> builder.registerMixin(Fields.class, FieldsMixin4.class)).create();
    assertThat(mapper.toString(new Fields("a")), is(equalTo("{}")));
    assertThat(mapper.parse("{\"a\":\"a\"}", Fields.class), is(equalTo(new Fields("a"))));
    assertThat(mapper.parse("{}", Fields.class), is(equalTo(new Fields(null))));
  }

  @Test
  public void test_names() {
    Json mapper = new DefaultJsonFactory().withModule((GsonModule) builder -> builder.registerMixin(Fields.class, FieldsMixin1.class)).create();
    assertThat(mapper.toString(new Names("one", 1)), is(equalTo("{\"key\":\"one\",\"value\":1}")));
    assertThat(mapper.parse("{\"key\":\"one\",\"value\":1}", Names.class), is(equalTo(new Names("one", 1))));
    assertThat(mapper.parse("{\"name\":\"one\",\"value\":1}", Names.class), is(equalTo(new Names(null, 1))));
    assertThat(mapper.parse("{\"label\":\"one\",\"value\":1}", Names.class), is(equalTo(new Names("one", 1))));
  }

  @Test
  public void test_expose() {
    Json mapper = new DefaultJsonFactory().withModule((GsonModule) builder -> builder.registerMixin(Point.class, PointMixin1.class)).create();
    assertThat(mapper.toString(new Point()), is(equalTo("{\"x\":1,\"y\":2}")));
  }

  public static class PointMixin1 {
    @Expose(serialize = false)
    int id;
  }

  public static class Point {
    int x = 1;
    int y = 2;
    int id = 1234;
  }

  public static class Names {
    @SerializedName(value = "key", alternate = {"label"})
    String name;
    int value;

    private Names() {}

    public Names(String name, int value) {
      this.name = name;
      this.value = value;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Names names = (Names) o;
      return value == names.value && Objects.equals(name, names.name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, value);
    }
  }

  public static class Fields {
    String a;

    public Fields() {}

    public Fields(String a) {
      this.a = a;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Fields fields = (Fields) o;
      return Objects.equals(a, fields.a);
    }

    @Override
    public int hashCode() {
      return Objects.hash(a);
    }
  }

  public static class FieldsMixin1 {
    @Expose(serialize = true, deserialize = true)
    String a;
  }

  public static class FieldsMixin2 {
    @Expose(serialize = false, deserialize = false)
    String a;
  }

  public static class FieldsMixin3 {
    @Expose(serialize = true, deserialize = false)
    String a;
  }

  public static class FieldsMixin4 {
    @Expose(serialize = false, deserialize = true)
    String a;
  }

  public static class BazMixin2 {
    private TimeUnit body;
  }

  public static class BarMixin2 {
    private Shape shape;
  }

  public static class BarMixin<T extends Shape> {
    @JsonAdapter(UnsafeObjectTypeAdapterFactory.class)
    private T shape;
  }

  public static class BazMixin<T> {
    @JsonAdapter(UnsafeObjectTypeAdapterFactory.class)
    private T body;
  }

  public static class Bar<T extends Shape> {
    private T shape;

    public Bar<T> setShape(T shape) {
      this.shape = shape;
      return this;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Bar<?> foo = (Bar<?>) o;
      return Objects.equals(shape, foo.shape);
    }

    @Override
    public int hashCode() {
      return Objects.hash(shape);
    }
  }

  public static class Baz<T> {
    private T body;

    public Baz<T> setBody(T body) {
      this.body = body;
      return this;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Baz<?> baz = (Baz<?>) o;
      return Objects.equals(body, baz.body);
    }

    @Override
    public int hashCode() {
      return Objects.hash(body);
    }
  }

  public interface Shape {
  }

  public static class Square implements Shape {
    protected int x;

    public Square() {
    }

    public Square(int x) {
      this.x = x;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Square square = (Square) o;
      return x == square.x;
    }

    @Override
    public int hashCode() {
      return Objects.hash(x);
    }
  }
}