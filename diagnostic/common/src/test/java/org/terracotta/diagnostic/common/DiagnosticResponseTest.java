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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;

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