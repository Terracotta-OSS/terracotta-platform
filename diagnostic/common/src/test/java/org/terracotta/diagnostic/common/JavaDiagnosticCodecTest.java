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
package org.terracotta.diagnostic.common;

import org.junit.Test;

import java.io.Closeable;
import java.io.Serializable;
import java.util.Objects;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

/**
 * @author Mathieu Carbou
 */
public class JavaDiagnosticCodecTest extends CommonCodecTest<byte[]> {

  public JavaDiagnosticCodecTest() {
    super("Java", new JavaDiagnosticCodec());
  }

  @Test
  public void test_serialize_failure() {
    assertThrows(DiagnosticCodecException.class, () -> codec.serialize(this));
  }

  @Test
  public void test_deserialize_failure() {
    assertThrows(DiagnosticCodecException.class, () -> codec.deserialize(new byte[0], getClass()));
  }

  @Test
  public void test_serialize_request() {
    DiagnosticRequest request = new DiagnosticRequest(Closeable.class, "prepareDiner()");
    byte[] bb = codec.serialize(request);
    assertThat(codec.deserialize(bb, DiagnosticRequest.class), is(equalTo(request)));
  }

  @Test
  public void test_serialize_request_with_complex_arguments() {
    Tomato tomato = new Tomato(new TomatoCooking(), "red");
    Pepper pepper = new Pepper(new TomatoCooking(), "spain");
    DiagnosticRequest request = new DiagnosticRequest(Closeable.class, "prepareDiner()", tomato, pepper);
    byte[] bb = codec.serialize(request);
    assertThat(codec.deserialize(bb, DiagnosticRequest.class), is(equalTo(request)));
  }

  public static abstract class Vegie<T extends CookingManual> implements Serializable {

    private static final long serialVersionUID = 8814453699890446378L;
    private final String color;
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

    private static final long serialVersionUID = -7682179521977903270L;
    private final String from;

    public Tomato(TomatoCooking cookingManual, String color) {
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

    private static final long serialVersionUID = -6094124039467431899L;
    private final String from;

    public Pepper(TomatoCooking cookingManual, String color) {
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

  public static abstract class CookingManual implements Serializable {
    private static final long serialVersionUID = -739654502153378205L;
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
    private static final long serialVersionUID = -3368019336592029331L;
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
