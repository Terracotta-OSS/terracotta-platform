/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.client;

import com.terracottatech.nomad.server.NomadServer;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RunWith(MockitoJUnitRunner.class)
public abstract class NomadClientProcessTest {
  @Mock
  protected NomadServer server1;

  @Mock
  protected NomadServer server2;

  protected Set<NamedNomadServer> servers;

  @Before
  public void before() {
    servers = Stream.of(
        new NamedNomadServer("server1", server1),
        new NamedNomadServer("server2", server2)
    ).collect(Collectors.toSet());
  }
}
