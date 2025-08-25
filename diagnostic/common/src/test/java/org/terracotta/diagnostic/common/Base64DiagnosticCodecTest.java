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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.internal.matchers.ThrowableMessageMatcher.hasMessage;

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
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> codec.deserialize("", getClass()));
    assertThat(e, hasMessage(equalTo("Target type must be assignable from String or byte[]")));
  }

}
