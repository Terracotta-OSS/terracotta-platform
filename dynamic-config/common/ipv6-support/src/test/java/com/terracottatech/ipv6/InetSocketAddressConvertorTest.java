/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.ipv6;

import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.List;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertThat;

public class InetSocketAddressConvertorTest {
  @Test
  public void validateHostPortList_ok_hostname() {
    List<InetSocketAddress> validHosts = InetSocketAddressConvertor.getInetSocketAddresses(
        new String[] {
            "some-host:9510", //Hostname containing '-' and port
            "some-host", //Hostname containing '-' only
            "pif:9610", //Simple hostname and port
            "piaf22:9710" //Hostname containing numbers and port
        });
    InetSocketAddress server1 = InetSocketAddress.createUnresolved("some-host", 9510);
    InetSocketAddress server2 = InetSocketAddress.createUnresolved("some-host", 0);
    InetSocketAddress server3 = InetSocketAddress.createUnresolved("pif", 9610);
    InetSocketAddress server4 = InetSocketAddress.createUnresolved("piaf22", 9710);
    assertThat(validHosts, hasItems(server1, server2, server3, server4));
  }

  @Test
  public void validateHostPortList_ok_ipv4_lookalike_hostname() {
    List<InetSocketAddress> validHosts = InetSocketAddressConvertor.getInetSocketAddresses(
        new String[] {
            "10.10.10",
            "10.10.10.10.10",
            "999.10.10.400",
            "10-10-10-10"
        });
    InetSocketAddress server1 = InetSocketAddress.createUnresolved("10.10.10", 0);
    InetSocketAddress server2 = InetSocketAddress.createUnresolved("10.10.10.10.10", 0);
    InetSocketAddress server3 = InetSocketAddress.createUnresolved("999.10.10.400", 0);
    InetSocketAddress server4 = InetSocketAddress.createUnresolved("10-10-10-10", 0);
    assertThat(validHosts, hasItems(server1, server2, server3, server4));
  }

  @Test(expected = IllegalArgumentException.class)
  public void validateHostPortList_notok_hostname_1() {
    InetSocketAddressConvertor.getInetSocketAddress("some_host");
  }

  @Test(expected = IllegalArgumentException.class)
  public void validateHostPortList_notok_hostname_2() {
    InetSocketAddressConvertor.getInetSocketAddress("%%%@@^^***");
  }

  @Test
  public void validateHostPortList_ok_ipv4() {
    List<InetSocketAddress> validServers = InetSocketAddressConvertor.getInetSocketAddresses(
        new String[] {
            "10.12.14.43:9610", //Address and port
            "10.12.14.43", //Address only
            "127.0.0.1:9510", //Loopback address and port
            "127.0.0.1" //Loopback address only
        });
    InetSocketAddress server1 = InetSocketAddress.createUnresolved("10.12.14.43", 9610);
    InetSocketAddress server2 = InetSocketAddress.createUnresolved("10.12.14.43", 0);
    InetSocketAddress server3 = InetSocketAddress.createUnresolved("127.0.0.1", 9510);
    InetSocketAddress server4 = InetSocketAddress.createUnresolved("127.0.0.1", 0);
    assertThat(validServers, hasItems(server1, server2, server3, server4));
  }

  @Test(expected = IllegalArgumentException.class)
  public void validateHostPortList_notok_ipv4_1() {
    InetSocketAddressConvertor.getInetSocketAddress("abc.10.10.%%^");
  }

  @Test(expected = IllegalArgumentException.class)
  public void validateHostPortList_notok_ipv4_2() {
    InetSocketAddressConvertor.getInetSocketAddress("");
  }

  @Test(expected = IllegalArgumentException.class)
  public void validateHostPortList_notok_ipv4_3() {
    InetSocketAddressConvertor.getInetSocketAddress(":9510");
  }

  @Test
  public void validateHostPortList_ok_ipv6() {
    List<InetSocketAddress> validServers = InetSocketAddressConvertor.getInetSocketAddresses(
        new String[] {
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

    InetSocketAddress server1 = InetSocketAddress.createUnresolved("[2001:db8:a0b:12f0:0:0:0:1]", 9510);
    InetSocketAddress server2 = InetSocketAddress.createUnresolved("2001:db8:a0b:12f0:0:0:0:1", 0);
    InetSocketAddress server3 = InetSocketAddress.createUnresolved("[2001:db8:a0b:12f0:0:0:0:1]", 0);
    InetSocketAddress server4 = InetSocketAddress.createUnresolved("[2001:db8:a0b:12f0::1]", 9510);
    InetSocketAddress server5 = InetSocketAddress.createUnresolved("2001:db8:a0b:12f0::1", 0);
    InetSocketAddress server6 = InetSocketAddress.createUnresolved("[::1]", 9510);
    InetSocketAddress server7 = InetSocketAddress.createUnresolved("::1", 0);
    InetSocketAddress server8 = InetSocketAddress.createUnresolved("[::1]", 0);
    InetSocketAddress server9 = InetSocketAddress.createUnresolved("[2001:db8::]", 9510);
    InetSocketAddress server10 = InetSocketAddress.createUnresolved("2001:db8::", 0);
    assertThat(validServers, hasItems(server1, server2, server3, server4, server5, server6, server7, server8, server9, server10));
  }

  @Test(expected = IllegalArgumentException.class)
  public void validateHostPortList_notok_ipv6_1() {
    //Too short
    InetSocketAddressConvertor.getInetSocketAddress("aaaa:aaaa:aaaa:aaaa");
  }

  @Test(expected = IllegalArgumentException.class)
  public void validateHostPortList_notok_ipv6_2() {
    //Too long
    InetSocketAddressConvertor.getInetSocketAddress("aaaa:aaaa:aaaa:aaaa:aaaa:aaaa:aaaa:aaaa:aaaa:aaaa:aaaa:aaaa");
  }

  @Test(expected = IllegalArgumentException.class)
  public void validateHostPortList_notok_ipv6_3() {
    //Correct length but invalid characters
    InetSocketAddressConvertor.getInetSocketAddress("zzzz:aaaa:aaaa:nnnn:aaaa:aaaa:aaaa:zzzz");
  }

  @Test(expected = IllegalArgumentException.class)
  public void validateHostPortList_notok_ipv6_4() {
    //Multiple double colons
    InetSocketAddressConvertor.getInetSocketAddress("2001:db8:a0b::12f0::1");
  }

  @Test(expected = IllegalArgumentException.class)
  public void validateHostPortList_notok_ipv6_5() {
    //Triple colon
    InetSocketAddressConvertor.getInetSocketAddress("2001:db8:a0b:12f0:::1");
  }

  @Test(expected = IllegalArgumentException.class)
  public void validateHostPortList_notok_ipv6_6() {
    //IPv6 address and port without enclosing the IP in square brackets
    InetSocketAddressConvertor.getInetSocketAddress("2001:1:1:1:1:1:1:1:9510");
  }

  @Test(expected = IllegalArgumentException.class)
  public void validateHostPortList_notok_ipv6_7() {
    //Only port
    InetSocketAddressConvertor.getInetSocketAddress("[]:9510");
  }

  @Test(expected = IllegalArgumentException.class)
  public void validateHostPortList_notok_too_many_ports_1() {
    //Too many ports with hostname
    InetSocketAddressConvertor.getInetSocketAddress("host:12:12:1212");
  }

  @Test(expected = IllegalArgumentException.class)
  public void validateHostPortList_notok_too_many_ports_2() {
    //Too many ports with IPv4 address
    InetSocketAddressConvertor.getInetSocketAddress("10.10.10.10:12:12");
  }

  @Test(expected = IllegalArgumentException.class)
  public void validateHostPortList_notok_too_many_ports_3() {
    //Too many ports with IPv6 address
    InetSocketAddressConvertor.getInetSocketAddress("2001:db8:a0b:12f0:0:0:0:1:12:12");
  }

  @Test(expected = IllegalArgumentException.class)
  public void validateHostPortList_notok_faulty_port_1() {
    //Port out of allowed range
    InetSocketAddressConvertor.getInetSocketAddress("host:12121212");
  }

  @Test(expected = IllegalArgumentException.class)
  public void validateHostPortList_notok_faulty_port_2() {
    //Port out of allowed range
    InetSocketAddressConvertor.getInetSocketAddress("host:-1");
  }

  @Test(expected = IllegalArgumentException.class)
  public void validateHostPortList_notok_faulty_port_3() {
    //port not a number
    InetSocketAddressConvertor.getInetSocketAddress("host:blah");
  }
}