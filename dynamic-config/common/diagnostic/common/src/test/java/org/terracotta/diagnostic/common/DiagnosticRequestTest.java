/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.diagnostic.common;

import org.junit.Test;

import java.io.Closeable;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

/**
 * @author Mathieu Carbou
 */
public class DiagnosticRequestTest {

  @Test(expected = NullPointerException.class)
  public void test_ctor_null_1() {
    new DiagnosticRequest(null, "");
  }

  @Test(expected = NullPointerException.class)
  public void test_ctor_null_2() {
    new DiagnosticRequest(Closeable.class, null);
  }

  @Test
  public void test_ctor() {
    DiagnosticRequest request = new DiagnosticRequest(Closeable.class, "", (Object[]) null);
    assertThat(request.getArguments(), is(emptyArray()));
  }

  @Test
  public void test_args_not_in_toString() {
    DiagnosticRequest request = new DiagnosticRequest(Closeable.class, "run", 1, 2, 3);
    assertThat(request.toString(), is(equalTo("DiagnosticRequest{serviceInterface='java.io.Closeable', methodName='run'}")));
  }

  @Test
  public void test_equals_considers_everything() {
    DiagnosticRequest request1 = new DiagnosticRequest(Closeable.class, "run", 1, 2, 3);
    DiagnosticRequest request2 = new DiagnosticRequest(Closeable.class, "run", 1, 2, 3);
    DiagnosticRequest request3 = new DiagnosticRequest(Closeable.class, "run", 1, 2, 3L);
    assertThat(request1, is(equalTo(request2)));
    assertThat(request1, is(not(equalTo(request3))));
  }

  @Test
  public void test_hashCode_considers_everything() {
    DiagnosticRequest request1 = new DiagnosticRequest(Closeable.class, "run", 1, 2, 3);
    DiagnosticRequest request2 = new DiagnosticRequest(Closeable.class, "run", 1, 2, 3);
    DiagnosticRequest request3 = new DiagnosticRequest(Closeable.class, "run", 1, 2, 4);
    assertThat(request1.hashCode(), is(equalTo(request2.hashCode())));
    assertThat(request1.hashCode(), is(not(equalTo(request3.hashCode()))));
  }

}
