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

import java.io.Closeable;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.not;

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
