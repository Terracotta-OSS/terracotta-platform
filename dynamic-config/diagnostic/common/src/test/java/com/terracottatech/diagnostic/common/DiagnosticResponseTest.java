/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.diagnostic.common;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * @author Mathieu Carbou
 */
public class DiagnosticResponseTest {

  @Test
  public void test_ctor() {
    DiagnosticResponse<?> response = new DiagnosticResponse<>(null, new IllegalArgumentException("foo"));
    assertThat(response.getBody(), is(nullValue()));
    assertThat(response.getError().get(), containsString(IllegalArgumentException.class.getName() + ": foo"));
    assertThat(response.getErrorType().get(), is(equalTo(IllegalArgumentException.class.getName())));
    assertThat(response.getErrorStack().get(), containsString("foo"));

    response = new DiagnosticResponse<>(null, new IllegalArgumentException());
    assertThat(response.getBody(), is(nullValue()));
    assertThat(response.getError().get(), containsString(IllegalArgumentException.class.getName()));
    assertThat(response.getErrorType().get(), is(equalTo(IllegalArgumentException.class.getName())));
    assertThat(response.getErrorStack().isPresent(), is(true));

    // body can be null
    response = new DiagnosticResponse<>(null);
    assertThat(response.getBody(), is(nullValue()));
    assertThat(response.getError().isPresent(), is(false));
    assertThat(response.getErrorType().isPresent(), is(false));
    assertThat(response.getErrorStack().isPresent(), is(false));

    response = new DiagnosticResponse<>("foo");
    assertThat(response.getBody(), is(equalTo("foo")));
    assertThat(response.getError().isPresent(), is(false));
    assertThat(response.getErrorType().isPresent(), is(false));
    assertThat(response.getErrorStack().isPresent(), is(false));
  }

  @Test
  public void test_equals_considers_everything() {
    DiagnosticResponse<String> request1 = new DiagnosticResponse<>("foo");
    DiagnosticResponse<String> request2 = new DiagnosticResponse<>("foo");
    Throwable error = new Throwable("");
    DiagnosticResponse<String> request3 = new DiagnosticResponse<>(null, error);
    DiagnosticResponse<String> request4 = new DiagnosticResponse<>(null, error);
    assertThat(request1, is(equalTo(request2)));
    assertThat(request3, is(equalTo(request4)));
  }

  @Test
  public void test_hashCode_considers_everything() {
    DiagnosticResponse<String> request1 = new DiagnosticResponse<>("foo");
    DiagnosticResponse<String> request2 = new DiagnosticResponse<>("foo");
    Throwable error = new Throwable("");
    DiagnosticResponse<String> request3 = new DiagnosticResponse<>(null, error);
    DiagnosticResponse<String> request4 = new DiagnosticResponse<>(null, error);
    assertThat(request1.hashCode(), is(equalTo(request2.hashCode())));
    assertThat(request3.hashCode(), is(equalTo(request4.hashCode())));
  }

}