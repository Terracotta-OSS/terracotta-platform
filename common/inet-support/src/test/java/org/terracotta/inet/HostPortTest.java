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
package org.terracotta.inet;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.terracotta.testing.ExceptionMatcher.throwing;

public class HostPortTest {

  @Test
  public void hostPort_equalities() {
    HostPort server1 = HostPort.create("[2001:db8:a0b:12f0:0:0:0:1]", 1);
    HostPort server2 = HostPort.create("2001:db8:a0b:12f0:0:0:0:1", 1);
    HostPort server3 = HostPort.create("[2001:db8:a0b:12f0:0:0:0:1", 1);
    HostPort server4 = HostPort.create("2001:db8:a0b:12f0:0:0:0:1]", 1);
    assertThat(server1, allOf(equalTo(server2), equalTo(server3), equalTo(server4)));
    assertHostPortException("");
    assertHostPortException("[]");
    assertHostPortException("[");
    assertHostPortException("]");
  }

  private void assertHostPortException(String host) {
    assertThat(
        () -> HostPort.create(host, 1),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(containsString("host name can not be blank"))));
  }
}
