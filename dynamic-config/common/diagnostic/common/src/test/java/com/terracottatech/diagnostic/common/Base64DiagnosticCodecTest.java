/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.diagnostic.common;

import org.junit.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Mathieu Carbou
 */
public class Base64DiagnosticCodecTest extends CommonCodecTest<String> {

  public Base64DiagnosticCodecTest() {
    super("Base64", new Base64DiagnosticCodec());
  }

  @Test
  public void test_serialize() {
    assertThat(codec.serialize(""), is(equalTo("")));
    assertThat(codec.serialize("".getBytes(UTF_8)), is(equalTo("")));
    assertThat(codec.serialize(" "), is(equalTo("IA==")));
    assertThat(codec.serialize(" ".getBytes(UTF_8)), is(equalTo("IA==")));
    assertThat(codec.serialize("foo"), is(equalTo("Zm9v")));
    assertThat(codec.serialize("a string with space breaking diagnostic handler"), is(equalTo("YSBzdHJpbmcgd2l0aCBzcGFjZSBicmVha2luZyBkaWFnbm9zdGljIGhhbmRsZXI=")));
  }

  @Test
  public void test_deserialize() {
    assertThat(codec.deserialize("", String.class), is(equalTo("")));
    assertThat(codec.deserialize("IA==", String.class), is(equalTo(" ")));
    assertThat(codec.deserialize("Zm9v", String.class), is(equalTo("foo")));
  }

  @Test
  public void test_deserialize_type_right() {
    for (Class<?> ifce : String.class.getInterfaces()) {
      assertThat(codec.deserialize("", ifce), is(equalTo("")));
    }
    for (Class<?> c = String.class.getSuperclass(); c != null; c = c.getSuperclass()) {
      assertThat(codec.deserialize("", c), is(equalTo("")));
    }
    assertThat(codec.deserialize("", byte[].class), is(equalTo(new byte[0])));
  }

  @Test
  public void test_deserialize_type_wrong() {
    exception.expect(IllegalArgumentException.class);
    exception.expectMessage("Target type must be assignable from String");
    assertThat(codec.deserialize("", getClass()), is(equalTo("")));
  }

}
