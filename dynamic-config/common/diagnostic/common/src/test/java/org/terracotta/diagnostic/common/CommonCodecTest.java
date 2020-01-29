/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.diagnostic.common;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Mathieu Carbou
 */
public abstract class CommonCodecTest<E> {

  @Rule public ExpectedException exception = ExpectedException.none();

  final String name;
  final DiagnosticCodec<E> codec;

  CommonCodecTest(String name, DiagnosticCodec<E> codec) {
    this.name = name;
    this.codec = codec;
  }

  @Test
  public void test_toString() {
    assertThat(codec.toString(), is(equalTo(name)));
  }

  @Test(expected = NullPointerException.class)
  public void test_serialize_null() {
    codec.serialize(null);
  }

  @Test(expected = NullPointerException.class)
  public void test_deserialize_null_object() {
    codec.deserialize(null, Integer.class);
  }

  @Test(expected = NullPointerException.class)
  public void test_deserialize_null_type() {
    if (codec.getEncodedType() == String.class) {
      codec.deserialize(codec.getEncodedType().cast("1"), null);
    } else if (codec.getEncodedType() == byte[].class) {
      codec.deserialize(codec.getEncodedType().cast(new byte[0]), null);
    } else {
      fail();
    }
  }

}
