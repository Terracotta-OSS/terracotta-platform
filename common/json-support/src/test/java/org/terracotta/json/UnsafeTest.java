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

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.reflect.TypeToken;
import org.junit.Test;
import org.terracotta.json.gson.GsonModule;
import org.terracotta.json.gson.UnsafeObjectTypeAdapterFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

public class UnsafeTest {

  @Test
  public void test_annotated() {
    final Json mapper = new DefaultJsonFactory().withModule((GsonModule) builder -> {
      builder.registerSuperType(Shape.class).withSubtypes(Square.class, Rectangle.class);
      builder.registerUnsafeTypeAdapters(Object[].class);
      builder.allowClassLoading(Square.class, Rectangle.class, TimeUnit.class, Object[].class, ArrayList.class);
    }).create();

    final Object[] oo = {
        new Square(3),
        new Bar<>().setShape(new Square(3)),
        new Baz<>().setBody(new Square(3)),
        new Baz<>().setBody(TimeUnit.MILLISECONDS),
        new Array3(new Object[]{new Square(3), new Rectangle(1, 2), null, 1}),
        new Array3(new ArrayList<>(Arrays.asList(new Square(3), new Rectangle(1, 2), null, 1))),
    };
    final String[] jsons = {
        "{\"type\":\"Square\",\"x\":3}",
        "{\"shape\":{\"@class\":\"org.terracotta.json.UnsafeTest$Square\",\"type\":\"Square\",\"x\":3}}",
        "{\"body\":{\"@class\":\"org.terracotta.json.UnsafeTest$Square\",\"type\":\"Square\",\"x\":3}}",
        "{\"body\":[\"java.util.concurrent.TimeUnit\",\"MILLISECONDS\"]}",
        "{\"shapes\":[\"[Ljava.lang.Object;\",[{\"@class\":\"org.terracotta.json.UnsafeTest$Square\",\"type\":\"Square\",\"x\":3},{\"@class\":\"org.terracotta.json.UnsafeTest$Rectangle\",\"type\":\"Rectangle\",\"x\":1,\"y\":2},null,1]]}",
        "{\"shapes\":[\"java.util.ArrayList\",[{\"@class\":\"org.terracotta.json.UnsafeTest$Square\",\"type\":\"Square\",\"x\":3},{\"@class\":\"org.terracotta.json.UnsafeTest$Rectangle\",\"type\":\"Rectangle\",\"x\":1,\"y\":2},null,1]]}",
    };

    for (int i = 0; i < oo.length; i++) {
      assertThat(String.valueOf(i), mapper.toString(oo[i]), is(equalTo(jsons[i])));
      // in a map
      assertThat(String.valueOf(i), mapper.toString(singletonMap("shape", oo[i])), is(equalTo("{\"shape\":" + jsons[i] + "}")));
    }

    for (int i = 0; i < oo.length; i++) {
      assertThat(String.valueOf(i), mapper.parse(jsons[i], oo[i].getClass()), is(equalTo(oo[i])));
    }

    for (int i = 0; i < oo.length; i++) {
      Map<String, Object> parsed = mapper.parseObject("{\"map\":" + jsons[i] + "}");
      assertThat(String.valueOf(i), parsed.get("map"), is(instanceOf(Map.class)));
    }
  }

  @Test
  public void test_raw_arrays_and_collections() {
    final Json mapper = new DefaultJsonFactory().withModule((GsonModule) builder -> {
      builder.registerSuperType(Shape.class).withSubtypes(Square.class, Rectangle.class);
      builder.registerUnsafeTypeAdapters(Object[].class);
      builder.registerUnsafeTypeAdapters(new TypeToken<Collection<?>>() {});
      builder.registerUnsafeTypeAdapters(new TypeToken<ArrayList<?>>() {});
      builder.allowClassLoading(Square.class, Rectangle.class, Object[].class, ArrayList.class, Collection.class);
    }).create();

    final Object[] oo = {
        new Array1(new Square(3), new Rectangle(1, 2), null, "some primitive"),
        new Array2(new Square(3), new Rectangle(1, 2), null, "some primitive"),
        new Array2Bis(new Square(3), new Rectangle(1, 2), null, "some primitive"),
    };
    final String[] jsons = {
        "{\"shapes\":[{\"@class\":\"org.terracotta.json.UnsafeTest$Square\",\"type\":\"Square\",\"x\":3},{\"@class\":\"org.terracotta.json.UnsafeTest$Rectangle\",\"type\":\"Rectangle\",\"x\":1,\"y\":2},null,\"some primitive\"]}",
        "{\"shapes\":[\"java.util.ArrayList\",[{\"@class\":\"org.terracotta.json.UnsafeTest$Square\",\"type\":\"Square\",\"x\":3},{\"@class\":\"org.terracotta.json.UnsafeTest$Rectangle\",\"type\":\"Rectangle\",\"x\":1,\"y\":2},null,\"some primitive\"]]}",
        "{\"shapes\":[{\"@class\":\"org.terracotta.json.UnsafeTest$Square\",\"type\":\"Square\",\"x\":3},{\"@class\":\"org.terracotta.json.UnsafeTest$Rectangle\",\"type\":\"Rectangle\",\"x\":1,\"y\":2},null,\"some primitive\"]}",
    };

    for (int i = 0; i < oo.length; i++) {
      assertThat(String.valueOf(i), mapper.toString(oo[i]), is(equalTo(jsons[i])));
      // in a map
      assertThat(mapper.toString(singletonMap("map", oo[i])), is(equalTo("{\"map\":" + jsons[i] + "}")));
    }

    for (int i = 0; i < oo.length; i++) {
      assertThat(String.valueOf(i), mapper.parse(jsons[i], oo[i].getClass()), is(equalTo(oo[i])));
    }

    for (int i = 0; i < oo.length; i++) {
      Map<String, Object> parsed = mapper.parseObject("{\"map\":" + jsons[i] + "}");
      assertThat(parsed.get("map"), is(instanceOf(Map.class)));
    }
  }

  public static class Array1 {
    Object[] shapes;

    // For Json
    private Array1() {}

    public Array1(Object... shapes) {
      this.shapes = shapes;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Array1 array1 = (Array1) o;
      return Arrays.deepEquals(shapes, array1.shapes);
    }

    @Override
    public int hashCode() {
      return Arrays.hashCode(shapes);
    }
  }

  public static class Array2 {
    Collection<?> shapes;

    // For Json
    private Array2() {}

    public Array2(Object... shapes) {
      this.shapes = new ArrayList<>(Arrays.asList(shapes));
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Array2 array2 = (Array2) o;
      return Objects.deepEquals(shapes, array2.shapes);
    }

    @Override
    public int hashCode() {
      return Objects.hash(shapes);
    }
  }

  public static class Array2Bis {
    ArrayList<?> shapes;

    // For Json
    private Array2Bis() {}

    public Array2Bis(Object... shapes) {
      this.shapes = new ArrayList<>(Arrays.asList(shapes));
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Array2Bis array2 = (Array2Bis) o;
      return Objects.deepEquals(shapes, array2.shapes);
    }

    @Override
    public int hashCode() {
      return Objects.hash(shapes);
    }
  }

  public static class Array3 {
    @JsonAdapter(UnsafeObjectTypeAdapterFactory.class)
    Object shapes;

    // For Json
    private Array3() {}

    public Array3(Object o) {
      this.shapes = o;
      ;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Array3 array3 = (Array3) o;
      return Objects.deepEquals(shapes, array3.shapes);
    }

    @Override
    public int hashCode() {
      return Objects.hash(shapes);
    }
  }

  public static class Bar<T extends Shape> {
    @JsonAdapter(UnsafeObjectTypeAdapterFactory.class)
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
    @JsonAdapter(UnsafeObjectTypeAdapterFactory.class)
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