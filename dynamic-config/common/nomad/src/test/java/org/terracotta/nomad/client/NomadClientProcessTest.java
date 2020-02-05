/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.nomad.client;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.terracotta.nomad.server.NomadServer;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@RunWith(MockitoJUnitRunner.class)
public abstract class NomadClientProcessTest {
  @Mock
  protected NomadServer<String> server1;

  @Mock
  protected NomadServer<String> server2;

  protected List<NomadEndpoint<String>> servers;

  protected InetSocketAddress address1 = InetSocketAddress.createUnresolved("localhost", 9410);
  protected InetSocketAddress address2 = InetSocketAddress.createUnresolved("localhost", 9411);

  @Before
  public void before() {
    servers = Stream.of(
        new NomadEndpoint<>(address1, server1),
        new NomadEndpoint<>(address2, server2)
    ).collect(toList());
  }
}