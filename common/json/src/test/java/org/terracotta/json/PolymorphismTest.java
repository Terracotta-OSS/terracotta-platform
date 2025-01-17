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

import com.google.gson.reflect.TypeToken;
import org.junit.Test;
import org.terracotta.json.gson.GsonModule;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

public class PolymorphismTest {

  @Test
  public void test_default() {
    final Json mapper = new DefaultJsonFactory().withModule((GsonModule) builder -> builder.registerSuperType(Shape.class).withSubtypes(Square.class, Rectangle.class)).create();

    final Shape[] shapes = {
        new Square(3),
        new Rectangle(2, 3),
    };
    final String[] jsons = {
        "{\"type\":\"Square\",\"x\":3}",
        "{\"type\":\"Rectangle\",\"x\":2,\"y\":3}",
    };

    for (int i = 0; i < shapes.length; i++) {
      assertThat(mapper.toString(shapes[i]), is(equalTo(jsons[i])));
      assertThat(mapper.parse(jsons[i], shapes[i].getClass()), is(equalTo(shapes[i])));

      // in a map
      assertThat(mapper.toString(singletonMap("shape", shapes[i])), is(equalTo("{\"shape\":" + jsons[i] + "}")));

      // Gson is not able to read a @type attribute when the destination type is object. It has to be wrapped.
      Map<String, Object> parsed = mapper.parseObject("{\"shape\":" + jsons[i] + "}");
      assertThat(parsed.get("shape"), is(instanceOf(Map.class)));
    }
  }

  @Test
  public void test_custom_mapping() {
    final Json mapper = new DefaultJsonFactory().withModule((GsonModule) builder -> {
      builder.registerSuperType(Shape.class, "@type");
      builder.registerSubtype(Shape.class, Square.class, "SQUARE");
      builder.registerSubtype(Shape.class, Rectangle.class, "RECTANGLE");
    }).create();

    final Shape[] shapes = {
        new Square(3),
        new Rectangle(2, 3),
    };
    final String[] jsons = {
        "{\"@type\":\"SQUARE\",\"x\":3}",
        "{\"@type\":\"RECTANGLE\",\"x\":2,\"y\":3}",
    };

    for (int i = 0; i < shapes.length; i++) {
      assertThat(mapper.toString(shapes[i]), is(equalTo(jsons[i])));
      assertThat(mapper.parse(jsons[i], shapes[i].getClass()), is(equalTo(shapes[i])));

      // in a map
      assertThat(mapper.toString(singletonMap("shape", shapes[i])), is(equalTo("{\"shape\":" + jsons[i] + "}")));

      // Gson is not able to read a @class attribute when the destination type is object. It has to be wrapped.
      Map<String, Object> parsed = mapper.parseObject("{\"shape\":" + jsons[i] + "}");
      assertThat(parsed.get("shape"), is(instanceOf(Map.class)));
    }
  }

  @Test
  public void test_arrays() {
    final Json mapper = new DefaultJsonFactory().withModule((GsonModule) builder -> builder.registerSuperType(Shape.class).withSubtypes(Shape.class, Square.class, Rectangle.class)).create();

    final Object[] shapes = {
        new Array(new Square(3), new Rectangle(2, 3))
    };
    final String[] jsons = {
        "{\"shapes1\":[{\"type\":\"Square\",\"x\":3},{\"type\":\"Rectangle\",\"x\":2,\"y\":3}],\"shapes2\":[{\"type\":\"Square\",\"x\":3},{\"type\":\"Rectangle\",\"x\":2,\"y\":3}]}",
    };

    for (int i = 0; i < shapes.length; i++) {
      assertThat(mapper.toString(shapes[i]), is(equalTo(jsons[i])));
      assertThat(mapper.parse(jsons[i], shapes[i].getClass()), is(equalTo(shapes[i])));

      // in a map
      assertThat(mapper.toString(singletonMap("map", shapes[i])), is(equalTo("{\"map\":" + jsons[i] + "}")));

      // Gson is not able to read a @type attribute when the destination type is object. It has to be wrapped.
      Map<String, Object> parsed = mapper.parseObject("{\"map\":" + jsons[i] + "}");
      assertThat(parsed.get("map"), is(instanceOf(Map.class)));
    }
  }

  @Test
  public void test_superType_variable() {
    final Json mapper = new DefaultJsonFactory().withModule((GsonModule) builder -> builder.registerSuperType(Shape.class).withSubtypes(Square.class, Rectangle.class)).create();

    final Foo foo = new Foo().setShape(new Rectangle(2, 3));
    String json = "{\"shape\":{\"type\":\"Rectangle\",\"x\":2,\"y\":3}}";
    assertThat(mapper.toString(foo), is(equalTo(json)));
    assertThat(mapper.parse(json, Foo.class), is(equalTo(foo)));
  }

  @Test
  public void test_superType_as_class_type_parameter() {
    final Json mapper = new DefaultJsonFactory().withModule((GsonModule) builder -> {
      builder.registerSuperType(Shape.class, "@type");
      builder.registerSubtype(Shape.class, Rectangle.class, "RECTANGLE");
    }).create();

    final Bar<Rectangle> bar = new Bar<Rectangle>().setShape(new Rectangle(2, 3));
    String json = "{\"shape\":{\"@type\":\"RECTANGLE\",\"x\":2,\"y\":3}}";
    assertThat(mapper.toString(bar), is(equalTo(json)));
    assertThat(mapper.parse(json, new TypeToken<Bar<Rectangle>>() {}.getType()), is(equalTo(bar)));
  }

  @Test
  public void test_specifically_serialize_wildcard_super_type() {
    Json mapper = new DefaultJsonFactory().create();
    assertThat(mapper.toString(new A()), is(equalTo("{\"shapes\":[{}]}")));
    assertThat(mapper.toString(new B()), is(equalTo("{\"cars\":[{}]}")));

    mapper = new DefaultJsonFactory().withModule((GsonModule) builder -> builder.serializeSubtypes(Shape.class)).create();
    assertThat(mapper.toString(new A()), is(equalTo("{\"shapes\":[{\"x\":1,\"y\":2}]}")));
    assertThat(mapper.toString(new B()), is(equalTo("{\"cars\":[{}]}")));
  }

  public static class A {
    Collection<? extends Shape> shapes = singletonList(new Rectangle(1, 2));
  }

  public static class B {
    Collection<? extends Car> cars = singletonList(new Zoe());
  }

  interface Car {}

  static class Zoe implements Car {
    boolean ev = true;
  }

  public static class Array {
    Shape[] shapes1;
    Collection<Shape> shapes2;

    // For Json
    private Array() {}

    public Array(Shape... shapes) {
      this.shapes1 = shapes;
      this.shapes2 = Arrays.asList(shapes);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Array array = (Array) o;
      return Arrays.equals(shapes1, array.shapes1) && Objects.equals(shapes2, array.shapes2);
    }

    @Override
    public int hashCode() {
      int result = Objects.hash(shapes2);
      result = 31 * result + Arrays.hashCode(shapes1);
      return result;
    }
  }

  public static class Foo {
    private Shape shape;

    public Foo setShape(Shape shape) {
      this.shape = shape;
      return this;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Foo foo = (Foo) o;
      return Objects.equals(shape, foo.shape);
    }

    @Override
    public int hashCode() {
      return Objects.hash(shape);
    }
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

  public static class Rectangle extends Square {
    protected int y;

    public Rectangle() {
    }

    public Rectangle(int x, int y) {
      super(x);
      this.y = y;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      if (!super.equals(o)) return false;
      Rectangle rectangle = (Rectangle) o;
      return y == rectangle.y;
    }

    @Override
    public int hashCode() {
      return Objects.hash(super.hashCode(), y);
    }
  }
}