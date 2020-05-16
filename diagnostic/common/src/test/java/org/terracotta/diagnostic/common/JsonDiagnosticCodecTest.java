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
package org.terracotta.diagnostic.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import org.terracotta.common.struct.json.StructJsonModule;
import org.terracotta.json.ObjectMapperFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.terracotta.common.struct.Tuple2.tuple2;

/**
 * @author Mathieu Carbou
 */
public class JsonDiagnosticCodecTest extends CommonCodecTest<String> {

  public JsonDiagnosticCodecTest() {
    super("Json", new JsonDiagnosticCodec(new ObjectMapperFactory().withModule(new StructJsonModule())));
  }

  @Test
  public void test_serialize_simple_types() {
    Stream.of(
        tuple2(Integer.MAX_VALUE, "" + 0x7fffffff),
        tuple2(Long.MAX_VALUE, "" + 0x7fffffffffffffffL),
        tuple2(Float.MAX_VALUE, "" + Float.MAX_VALUE),
        tuple2(Double.MAX_VALUE, "" + Double.MAX_VALUE),
        tuple2((byte) 1, "1"),
        tuple2(true, "true"),
        tuple2(TimeUnit.HOURS, "\"HOURS\"")
    ).forEach(tuple -> assertThat("Serializing: " + tuple.t1, codec.serialize(tuple.t1), is(equalTo(tuple.t2))));
  }

  @Test
  public void test_deserialize_simple_types() {
    Stream.of(
        tuple2(Integer.MAX_VALUE, "" + 0x7fffffff),
        tuple2(Long.MAX_VALUE, "" + 0x7fffffffffffffffL),
        tuple2(Float.MAX_VALUE, "" + Float.MAX_VALUE),
        tuple2(Double.MAX_VALUE, "" + Double.MAX_VALUE),
        tuple2((byte) 1, "1"),
        tuple2(true, "true"),
        tuple2(TimeUnit.HOURS, "\"HOURS\"")
    ).forEach(tuple -> {
      Class<?> target = tuple.t1 instanceof Enum<?> ? ((Enum<?>) tuple.t1).getDeclaringClass() : tuple.t1.getClass();
      assertThat("De-serializing: " + tuple.t2, codec.deserialize(tuple.t2, target), is(equalTo(tuple.t1)));
    });
  }

  @Test
  public void test_serialize_request() {
    DiagnosticRequest request = new DiagnosticRequest(Closeable.class, "prepareDiner()");
    assertThat(codec.serialize(request), is(equalTo(jsonFile("/output1.json"))));
  }

  @Test
  public void test_deserialize_request() {
    DiagnosticRequest request = new DiagnosticRequest(Closeable.class, "prepareDiner()");
    assertThat(codec.deserialize(jsonFile("/output1.json"), DiagnosticRequest.class), is(equalTo(request)));
  }

  @Test
  public void test_deserialize_to_json_obj() {
    assertThat(codec.deserialize(jsonFile("/output1.json"), JsonNode.class).toString(), is(equalTo("{\"arguments\":[],\"methodName\":\"prepareDiner()\",\"serviceInterface\":\"java.io.Closeable\"}")));
    assertThat(codec.deserialize(jsonFile("/output1.json"), ObjectNode.class).toString(), is(equalTo("{\"arguments\":[],\"methodName\":\"prepareDiner()\",\"serviceInterface\":\"java.io.Closeable\"}")));
  }

  @Test
  public void test_deserialize_fails() {
    exception.expect(DiagnosticCodecException.class);
    codec.deserialize("foo", getClass());
  }

  @Test
  public void test_serialize_request_with_complex_arguments() {
    Tomato tomato = new Tomato(new TomatoCooking(), "red");
    Pepper pepper = new Pepper(new TomatoCooking(), "spain");
    DiagnosticRequest request = new DiagnosticRequest(Closeable.class, "prepareDiner()", tomato, pepper);
    assertThat(codec.serialize(request), is(equalTo(jsonFile("/output2.json"))));
  }

  @Test
  public void test_deserialize_request_with_complex_arguments() {
    Tomato tomato = new Tomato(new TomatoCooking(), "red");
    Pepper pepper = new Pepper(new TomatoCooking(), "spain");
    DiagnosticRequest request = new DiagnosticRequest(Closeable.class, "prepareDiner()", tomato, pepper);
    DiagnosticRequest deserialized = codec.deserialize(jsonFile("/output2.json"), DiagnosticRequest.class);
    assertThat(deserialized, is(equalTo(request)));
  }

  @Test
  public void test_serialize_response_with_complex_arguments() {
    Tomato tomato = new Tomato(new TomatoCooking(), "red");
    DiagnosticResponse<Tomato> response = new DiagnosticResponse<>(tomato);
    assertThat(codec.serialize(response), is(equalTo(jsonFile("/output3.json"))));
  }

  @Test
  public void test_deserialize_response_with_complex_arguments() {
    Tomato tomato = new Tomato(new TomatoCooking(), "red");
    DiagnosticResponse<Tomato> response = new DiagnosticResponse<>(tomato);
    DiagnosticResponse<?> deserialized = codec.deserialize(jsonFile("/output3.json"), DiagnosticResponse.class);
    assertThat(deserialized, is(equalTo(response)));
  }

  private String jsonFile(String filename) {
    try {
      return new ObjectMapper().readTree(getClass().getResource(filename)).toString();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static abstract class Vegie<T extends CookingManual> {

    private final String color;
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    private final T cookingManual;

    public Vegie(T cookingManual, String color) {
      this.cookingManual = cookingManual;
      this.color = color;
    }

    public String getColor() {
      return color;
    }

    public T getCookingManual() {
      return cookingManual;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Vegie<?> vegie = (Vegie<?>) o;
      return Objects.equals(color, vegie.color) &&
          Objects.equals(cookingManual, vegie.cookingManual);
    }

    @Override
    public int hashCode() {
      return Objects.hash(color, cookingManual);
    }
  }

  public static class Tomato extends Vegie<TomatoCooking> {

    private final String from;

    @JsonCreator
    public Tomato(@JsonProperty("cookingManual") TomatoCooking cookingManual,
                  @JsonProperty("color") String color) {
      super(cookingManual, color);
      from = "Canada";
    }

    public String getFrom() {
      return from;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      if (!super.equals(o)) return false;
      Tomato tomato = (Tomato) o;
      return Objects.equals(from, tomato.from);
    }

    @Override
    public int hashCode() {
      return Objects.hash(super.hashCode(), from);
    }
  }

  public static class Pepper extends Vegie<TomatoCooking> {

    private final String from;

    @JsonCreator
    public Pepper(@JsonProperty("cookingManual") TomatoCooking cookingManual,
                  @JsonProperty("color") String color) {
      super(cookingManual, color);
      from = "Canada";
    }

    public String getFrom() {
      return from;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      if (!super.equals(o)) return false;
      Pepper tomato = (Pepper) o;
      return Objects.equals(from, tomato.from);
    }

    @Override
    public int hashCode() {
      return Objects.hash(super.hashCode(), from);
    }
  }

  public static abstract class CookingManual {
    private int cookTime = 20;

    public int getCookTime() {
      return cookTime;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      CookingManual that = (CookingManual) o;
      return cookTime == that.cookTime;
    }

    @Override
    public int hashCode() {
      return Objects.hash(cookTime);
    }
  }

  public static class TomatoCooking extends CookingManual {
    private boolean saltNeeded;

    public boolean isSaltNeeded() {
      return saltNeeded;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      if (!super.equals(o)) return false;
      TomatoCooking that = (TomatoCooking) o;
      return saltNeeded == that.saltNeeded;
    }

    @Override
    public int hashCode() {
      return Objects.hash(super.hashCode(), saltNeeded);
    }
  }

}
