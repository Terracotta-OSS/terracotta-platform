/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.diagnostic.common;

import org.junit.Test;

import java.io.Closeable;
import java.io.Serializable;
import java.util.Objects;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Mathieu Carbou
 */
public class JavaDiagnosticCodecTest extends CommonCodecTest<byte[]> {

  public JavaDiagnosticCodecTest() {
    super("Java", new JavaDiagnosticCodec());
  }

  @Test
  public void test_serialize_failure() {
    exception.expect(DiagnosticCodecException.class);
    codec.serialize(this);
  }

  @Test
  public void test_deserialize_failure() {
    exception.expect(DiagnosticCodecException.class);
    codec.deserialize(new byte[0], getClass());
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
