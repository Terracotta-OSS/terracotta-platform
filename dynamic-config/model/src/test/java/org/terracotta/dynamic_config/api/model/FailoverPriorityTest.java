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
package org.terracotta.dynamic_config.api.model;

import org.junit.Test;

import java.util.stream.Stream;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.terracotta.dynamic_config.api.model.FailoverPriority.Type.AVAILABILITY;
import static org.terracotta.dynamic_config.api.model.FailoverPriority.Type.CONSISTENCY;
import static org.terracotta.dynamic_config.api.model.FailoverPriority.availability;
import static org.terracotta.dynamic_config.api.model.FailoverPriority.consistency;
import static org.terracotta.dynamic_config.api.model.FailoverPriority.valueOf;
import static org.terracotta.testing.ExceptionMatcher.throwing;

/**
 * @author Mathieu Carbou
 */
public class FailoverPriorityTest {

  private final String ERR = "failover-priority should be either 'availability', 'consistency', or 'consistency:N' (where 'N' is the voter count expressed as a positive integer)";

  @Test
  public void test_getType() {
    assertThat(consistency().getType(), is(equalTo(CONSISTENCY)));
    assertThat(availability().getType(), is(equalTo(AVAILABILITY)));
  }

  @Test
  public void test_getVoters() {
    assertThat(consistency().getVoters(), is(equalTo(0)));
    assertThat(consistency(2).getVoters(), is(equalTo(2)));
    assertThat(availability().getVoters(), is(equalTo(0)));
  }

  @Test
  public void test_equals() {
    assertThat(consistency(), is(equalTo(consistency())));
    assertThat(consistency(2), is(equalTo(consistency(2))));
    assertThat(availability(), is(equalTo(availability())));

    assertThat(availability(), is(not(equalTo(consistency()))));
    assertThat(consistency(), is(not(equalTo(consistency(2)))));
  }

  @Test
  public void test_hashCode() {
    assertThat(consistency().hashCode(), is(equalTo(consistency().hashCode())));
    assertThat(consistency(2).hashCode(), is(equalTo(consistency(2).hashCode())));
    assertThat(availability().hashCode(), is(equalTo(availability().hashCode())));
  }

  @Test
  public void test_toString() {
    assertThat(consistency().toString(), is(equalTo("consistency")));
    assertThat(consistency(2).toString(), is(equalTo("consistency:2")));
    assertThat(availability().toString(), is(equalTo("availability")));
  }

  @Test
  public void test_consistency() {
    assertThat(
        () -> consistency(-1),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo(ERR)))));
    assertThat(
        () -> consistency(0),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo(ERR)))));
  }

  @Test
  public void test_valueOf() {
    assertThat(valueOf("availability"), is(equalTo(availability())));
    assertThat(valueOf("consistency"), is(equalTo(consistency())));
    assertThat(valueOf("consistency:2"), is(equalTo(consistency(2))));

    Stream.of(
        "consistency:0",
        "foo",
        "availability:8",
        "availability:foo",
        "consistency:-1",
        "consistency:foo",
        "consistency:",
        "1:consistency",
        "consistency:1:2",
        ":",
        ":::",
        "consistency-1",
        "??" // :D
    ).forEach(value -> assertThat(
        value,
        () -> valueOf(value),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo(ERR)))))
    );
  }
}