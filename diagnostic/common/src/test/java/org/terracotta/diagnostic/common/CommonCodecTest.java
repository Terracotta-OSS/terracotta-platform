/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Mathieu Carbou
 */
public abstract class CommonCodecTest<E> {

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
