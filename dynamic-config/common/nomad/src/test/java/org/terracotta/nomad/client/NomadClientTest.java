/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.nomad.client;

import org.junit.Test;

import java.time.Clock;
import java.util.Collections;

public class NomadClientTest {
  @Test(expected = IllegalArgumentException.class)
  public void mustSpecifyServers() {
    new NomadClient<>(Collections.emptyList(), "host", "user", Clock.systemUTC());
  }
}
