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

import java.util.List;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertThat;

public class InetSocketAddressConverterTest {
  @Test
  public void validateHostPortList_ok_hostname() {
    List<HostPort> validHosts = InetSocketAddressConverter.getHostPorts(
        new String[]{
            "some-host:9510", //Hostname containing '-' and port
            "some-host", //Hostname containing '-' only
            "pif:9610", //Simple hostname and port
            "piaf22:9710" //Hostname containing numbers and port
        });
    HostPort server1 = HostPort.create("some-host", 9510);
    HostPort server2 = HostPort.create("some-host", 0);
    HostPort server3 = HostPort.create("pif", 9610);
    HostPort server4 = HostPort.create("piaf22", 9710);
    assertThat(validHosts, hasItems(server1, server2, server3, server4));
  }

  @Test
  public void validateHostPortList_ok_ipv4_lookalike_hostname() {
    List<HostPort> validHosts = InetSocketAddressConverter.getHostPorts(
        new String[]{
            "10.10.10",
            "10.10.10.10.10",
            "999.10.10.400",
            "10-10-10-10"
        });
    HostPort server1 = HostPort.create("10.10.10", 0);
    HostPort server2 = HostPort.create("10.10.10.10.10", 0);
    HostPort server3 = HostPort.create("999.10.10.400", 0);
    HostPort server4 = HostPort.create("10-10-10-10", 0);
    assertThat(validHosts, hasItems(server1, server2, server3, server4));
  }

  @Test(expected = IllegalArgumentException.class)
  public void validateHostPortList_notok_hostname_1() {
    InetSocketAddressConverter.getHostPort("some_host");
  }

  @Test(expected = IllegalArgumentException.class)
  public void validateHostPortList_notok_hostname_2() {
    InetSocketAddressConverter.getHostPort("%%%@@^^***");
  }

  @Test
  public void validateHostPortList_ok_ipv4() {
    List<HostPort> validServers = InetSocketAddressConverter.getHostPorts(
        new String[]{
            "10.12.14.43:9610", //Address and port
            "10.12.14.43", //Address only
            "127.0.0.1:9510", //Loopback address and port
            "127.0.0.1" //Loopback address only
        });
    HostPort server1 = HostPort.create("10.12.14.43", 9610);
    HostPort server2 = HostPort.create("10.12.14.43", 0);
    HostPort server3 = HostPort.create("127.0.0.1", 9510);
    HostPort server4 = HostPort.create("127.0.0.1", 0);
    assertThat(validServers, hasItems(server1, server2, server3, server4));
  }

  @Test(expected = IllegalArgumentException.class)
  public void validateHostPortList_notok_ipv4_1() {
    InetSocketAddressConverter.getHostPort("abc.10.10.%%^");
  }

  @Test(expected = IllegalArgumentException.class)
  public void validateHostPortList_notok_ipv4_2() {
    InetSocketAddressConverter.getHostPort("");
  }

  @Test(expected = IllegalArgumentException.class)
  public void validateHostPortList_notok_ipv4_3() {
    InetSocketAddressConverter.getHostPort(":9510");
  }

  @Test
  public void validateHostPortList_ok_ipv6() {
    List<HostPort> validServers = InetSocketAddressConverter.getHostPorts(
        new String[]{
            "[2001:db8:a0b:12f0:0:0:0:1]:9510", //Full address and port
            "2001:db8:a0b:12f0:0:0:0:1", //Full address only
            "[2001:db8:a0b:12f0:0:0:0:1]", //Full address enclosed in brackets
            "[2001:db8:a0b:12f0::1]:9510", //Shortened address and port
            "2001:db8:a0b:12f0::1", //Shortened address only
            "[::1]:9510", //Loopback address with port
            "::1", //Loopback address only
            "[::1]", //Loopback address enclosed in brackets
            "[2001:db8::]:9510", //Funny address with port
            "2001:db8::" //Funny address only
        });

    HostPort server1 = HostPort.create("[2001:db8:a0b:12f0:0:0:0:1]", 9510);
    HostPort server2 = HostPort.create("2001:db8:a0b:12f0:0:0:0:1", 0);
    HostPort server3 = HostPort.create("[2001:db8:a0b:12f0:0:0:0:1]", 0);
    HostPort server4 = HostPort.create("[2001:db8:a0b:12f0::1]", 9510);
    HostPort server5 = HostPort.create("2001:db8:a0b:12f0::1", 0);
    HostPort server6 = HostPort.create("[::1]", 9510);
    HostPort server7 = HostPort.create("::1", 0);
    HostPort server8 = HostPort.create("[::1]", 0);
    HostPort server9 = HostPort.create("[2001:db8::]", 9510);
    HostPort server10 = HostPort.create("2001:db8::", 0);
    assertThat(validServers, hasItems(server1, server2, server3, server4, server5, server6, server7, server8, server9, server10));
  }

  @Test(expected = IllegalArgumentException.class)
  public void validateHostPortList_notok_ipv6_1() {
    //Too short
    InetSocketAddressConverter.getHostPort("aaaa:aaaa:aaaa:aaaa");
  }

  @Test(expected = IllegalArgumentException.class)
  public void validateHostPortList_notok_ipv6_2() {
    //Too long
    InetSocketAddressConverter.getHostPort("aaaa:aaaa:aaaa:aaaa:aaaa:aaaa:aaaa:aaaa:aaaa:aaaa:aaaa:aaaa");
  }

  @Test(expected = IllegalArgumentException.class)
  public void validateHostPortList_notok_ipv6_3() {
    //Correct length but invalid characters
    InetSocketAddressConverter.getHostPort("zzzz:aaaa:aaaa:nnnn:aaaa:aaaa:aaaa:zzzz");
  }

  @Test(expected = IllegalArgumentException.class)
  public void validateHostPortList_notok_ipv6_4() {
    //Multiple double colons
    InetSocketAddressConverter.getHostPort("2001:db8:a0b::12f0::1");
  }

  @Test(expected = IllegalArgumentException.class)
  public void validateHostPortList_notok_ipv6_5() {
    //Triple colon
    InetSocketAddressConverter.getHostPort("2001:db8:a0b:12f0:::1");
  }

  @Test(expected = IllegalArgumentException.class)
  public void validateHostPortList_notok_ipv6_6() {
    //IPv6 address and port without enclosing the IP in square brackets
    InetSocketAddressConverter.getHostPort("2001:1:1:1:1:1:1:1:9510");
  }

  @Test(expected = IllegalArgumentException.class)
  public void validateHostPortList_notok_ipv6_7() {
    //Only port
    InetSocketAddressConverter.getHostPort("[]:9510");
  }

  @Test(expected = IllegalArgumentException.class)
  public void validateHostPortList_notok_too_many_ports_1() {
    //Too many ports with hostname
    InetSocketAddressConverter.getHostPort("host:12:12:1212");
  }

  @Test(expected = IllegalArgumentException.class)
  public void validateHostPortList_notok_too_many_ports_2() {
    //Too many ports with IPv4 address
    InetSocketAddressConverter.getHostPort("10.10.10.10:12:12");
  }

  @Test(expected = IllegalArgumentException.class)
  public void validateHostPortList_notok_too_many_ports_3() {
    //Too many ports with IPv6 address
    InetSocketAddressConverter.getHostPort("2001:db8:a0b:12f0:0:0:0:1:12:12");
  }

  @Test(expected = IllegalArgumentException.class)
  public void validateHostPortList_notok_faulty_port_1() {
    //Port out of allowed range
    InetSocketAddressConverter.getHostPort("host:12121212");
  }

  @Test(expected = IllegalArgumentException.class)
  public void validateHostPortList_notok_faulty_port_2() {
    //Port out of allowed range
    InetSocketAddressConverter.getHostPort("host:-1");
  }

  @Test(expected = IllegalArgumentException.class)
  public void validateHostPortList_notok_faulty_port_3() {
    //port not a number
    InetSocketAddressConverter.getHostPort("host:blah");
  }
}