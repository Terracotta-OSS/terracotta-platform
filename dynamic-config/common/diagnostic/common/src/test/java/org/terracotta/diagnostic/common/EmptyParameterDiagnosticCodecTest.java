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

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.terracotta.diagnostic.common.EmptyParameterDiagnosticCodec.EOF;

/**
 * @author Mathieu Carbou
 */
public class EmptyParameterDiagnosticCodecTest extends CommonCodecTest<String> {

  public EmptyParameterDiagnosticCodecTest() {
    super("Empty", new EmptyParameterDiagnosticCodec());
  }

  @Test
  public void test_serialize() {
    assertThat(codec.serialize(""), is(equalTo(EOF)));
    assertThat(codec.serialize(" "), is(equalTo(" " + EOF)));
    assertThat(codec.serialize("foo"), is(equalTo("foo" + EOF)));
  }

  @Test
  public void test_deserialize() {
    assertThat(codec.deserialize(EOF, String.class), is(equalTo("")));
    assertThat(codec.deserialize(" " + EOF, String.class), is(equalTo(" ")));
    assertThat(codec.deserialize("foo" + EOF, String.class), is(equalTo("foo")));
  }

  @Test
  public void test_deserialize_type_right() {
    for (Class<?> ifce : String.class.getInterfaces()) {
      assertThat(codec.deserialize(EOF, ifce), is(equalTo("")));
    }
    for (Class<?> c = String.class.getSuperclass(); c != null; c = c.getSuperclass()) {
      assertThat(codec.deserialize(EOF, c), is(equalTo("")));
    }
  }

  @Test
  public void test_deserialize_type_wrong() {
    exception.expect(IllegalArgumentException.class);
    exception.expectMessage("Target type must be assignable from String");
    assertThat(codec.deserialize(EOF, getClass()), is(equalTo("")));
  }

  @Test
  public void test_deserialize_message_wrong() {
    exception.expect(DiagnosticCodecException.class);
    exception.expectMessage("Unsupported encoded input");
    assertThat(codec.deserialize("", String.class), is(equalTo("")));
  }

}
