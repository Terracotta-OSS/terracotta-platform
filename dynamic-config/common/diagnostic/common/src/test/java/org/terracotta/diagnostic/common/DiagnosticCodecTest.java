/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
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
      doCallRealMethod().when(codec).around(any());
    });
  }

  @Test
  public void test_around() {
    DiagnosticCodec<String> composition = codec3.around(codec2).around(codec1);
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
    codec3.around(null);
  }

}