/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.command;

import com.terracottatech.diagnostic.client.connection.ConcurrencySizing;
import com.terracottatech.diagnostic.client.connection.DiagnosticServiceProvider;
import com.terracottatech.diagnostic.client.connection.MultiDiagnosticServiceConnectionFactory;
import com.terracottatech.dynamic_config.cli.connect.DynamicConfigNodeAddressDiscovery;
import com.terracottatech.dynamic_config.cli.connect.NodeAddressDiscovery;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.concurrent.TimeUnit;

/**
 * @author Mathieu Carbou
 */
@RunWith(MockitoJUnitRunner.class)
public class AttachCommandTest {

  @Test
  @Ignore
  public void test() {
    DiagnosticServiceProvider diagnosticServiceProvider = new DiagnosticServiceProvider("NEW-TOOL", 10, TimeUnit.SECONDS, null);
    MultiDiagnosticServiceConnectionFactory connectionFactory = new MultiDiagnosticServiceConnectionFactory(diagnosticServiceProvider, 10, TimeUnit.SECONDS, new ConcurrencySizing());
    NodeAddressDiscovery nodeAddressDiscovery = new DynamicConfigNodeAddressDiscovery(diagnosticServiceProvider, 10, TimeUnit.SECONDS);
    AttachCommand attach = new AttachCommand(nodeAddressDiscovery, connectionFactory);
    attach.setType(TopologyChangeCommand.Type.NODE);
    //attach.setDestination();
    //attach.setSources();
    attach.run();
  }

}