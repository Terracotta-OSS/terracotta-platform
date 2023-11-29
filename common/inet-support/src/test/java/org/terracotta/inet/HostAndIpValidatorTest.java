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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.terracotta.inet.HostAndIpValidator.isValidHost;
import static org.terracotta.inet.HostAndIpValidator.isValidIPv4;
import static org.terracotta.inet.HostAndIpValidator.isValidIPv4Cidr;
import static org.terracotta.inet.HostAndIpValidator.isValidIPv6;
import static org.terracotta.inet.HostAndIpValidator.isValidIPv6Cidr;

public class HostAndIpValidatorTest {
  @Test
  public void testIsValidHost() throws Exception {
    assertThat(isValidHost("hostname"), is(true));
    assertThat(isValidHost("host-name"), is(true));
    assertThat(isValidHost("host-name123"), is(true));

    assertThat(isValidHost("host_name"), is(false));
    assertThat(isValidHost("host%%name"), is(false));
    assertThat(isValidHost("host:name"), is(false));
  }

  @Test
  public void testIsValidIPv4() throws Exception {
    assertThat(isValidIPv4("10.10.10.10"), is(true));
    assertThat(isValidIPv4("0.0.0.0"), is(true));
    assertThat(isValidIPv4("127.0.0.1"), is(true));

    assertThat(isValidIPv4("10.10.10"), is(false));
    assertThat(isValidIPv4("10.10.10.10.10"), is(false));
    assertThat(isValidIPv4("400:400:400:400"), is(false));
    assertThat(isValidIPv4("abc.10.10.%%^"), is(false));
    assertThat(isValidIPv4("10-10-10-10"), is(false));
  }

  @Test
  public void testIsValidIPv4Cidr() throws Exception {
    assertThat(isValidIPv4Cidr("10.10.10.10/23"), is(true));
    assertThat(isValidIPv4Cidr("0.0.0.0/24"), is(true));
    assertThat(isValidIPv4Cidr("127.0.0.1/25"), is(true));

    assertThat(isValidIPv4Cidr("10.10.10.10/-1"), is(false));
    assertThat(isValidIPv4Cidr("500.10.10.10/32"), is(false));
    assertThat(isValidIPv4Cidr("10.10.10.10/50"), is(false));
    assertThat(isValidIPv4Cidr("10.10.10.10/blah"), is(false));
  }

  @Test
  public void testIsValidIPv6() throws Exception {
    assertThat(isValidIPv6("2001:db8:a0b:12f0:0:0:0:1", false), is(true));
    assertThat(isValidIPv6("2001:db8:a0b:12f0::1", false), is(true));
    assertThat(isValidIPv6("::1", false), is(true));
    assertThat(isValidIPv6("2001:db8::", false), is(true));
    assertThat(isValidIPv6("[2001:db8::]", true), is(true));

    assertThat(isValidIPv6("aaaa:aaaa:aaaa:aaaa", false), is(false));
    assertThat(isValidIPv6("aaaa:aaaa:aaaa:aaaa:aaaa:aaaa:aaaa:aaaa:aaaa:aaaa:aaaa:aaaa", false), is(false));
    assertThat(isValidIPv6("2001:db8:a0b::12f0::1", false), is(false));
    assertThat(isValidIPv6("2001:db8:a0b:12f0:::1", false), is(false));
    assertThat(isValidIPv6("[2001:db8:a0b:12f0:::1]", true), is(false));
  }

  @Test
  public void testIsValidIPv6Cidr() throws Exception {
    assertThat(isValidIPv6Cidr("2001:db8:a0b:12f0:0:0:0:1/123"), is(true));
    assertThat(isValidIPv6Cidr("2001:db8:a0b:12f0::1/124"), is(true));
    assertThat(isValidIPv6Cidr("::1/125"), is(true));
    assertThat(isValidIPv6Cidr("2001:db8::/126"), is(true));

    assertThat(isValidIPv6Cidr("2001:db8:a0b:12f0:0:0:0:1/133"), is(false));
    assertThat(isValidIPv6Cidr("2001:db8:a0b:12f0::1/-1"), is(false));
    assertThat(isValidIPv6Cidr("::1/blah"), is(false));
  }
}