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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

/**
 * @author Mathieu Carbou
 */
@RunWith(MockitoJUnitRunner.class)
public class DiagnosticCodecTest {

  @Mock DiagnosticCodec<String> codec1;
  @Mock DiagnosticCodec<String> codec2;
  @Mock DiagnosticCodec<String> codec3;

  @Before
  public void setUp() {
    when(codec1.serialize(1)).thenReturn("1");
    when(codec1.deserialize("1", Integer.class)).thenReturn(1);
    when(codec1.toString()).thenReturn("codec1");

    when(codec2.serialize("1")).thenReturn("2");
    when(codec2.deserialize("2", String.class)).thenReturn("1");
    when(codec2.toString()).thenReturn("codec2");

    when(codec3.serialize("2")).thenReturn("3");
    when(codec3.deserialize("3", String.class)).thenReturn("2");
    when(codec3.toString()).thenReturn("codec3");

    Stream.of(codec1, codec2, codec3).forEach(codec -> {
      when(codec.getEncodedType()).thenReturn(String.class);
    });
  }

  @Test
  public void test_around() {
    DiagnosticCodec<String> composition = DiagnosticCodec.around(codec2, codec3).around(codec1);
    assertThat(composition.serialize(1), is(equalTo("3")));
    assertThat(composition.deserialize("3", Integer.class), is(equalTo(1)));

    InOrder inOrder = inOrder(codec1, codec2, codec3);

    inOrder.verify(codec1).serialize(1);
    inOrder.verify(codec2).serialize("1");
    inOrder.verify(codec3).serialize("2");
    inOrder.verify(codec3).deserialize("3", String.class);
    inOrder.verify(codec2).deserialize("2", String.class);
    inOrder.verify(codec1).deserialize("1", Integer.class);

    assertThat(composition.toString(), is(equalTo("codec1 => codec2 => codec3")));
  }

  @Test(expected = NullPointerException.class)
  public void test_around_npe() {
    DiagnosticCodec<String> codec = new DiagnosticCodecSkeleton<String>(String.class) {
      @Override
      public String serialize(Object o) throws DiagnosticCodecException {
        fail();
        return null;
      }

      @Override
      public <T> T deserialize(String encoded, Class<T> target) throws DiagnosticCodecException {
        fail();
        return null;
      }
    };
    codec.around(null);
  }

}