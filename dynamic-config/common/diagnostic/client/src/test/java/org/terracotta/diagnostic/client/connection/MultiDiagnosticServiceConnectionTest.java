/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.diagnostic.client.connection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.terracotta.diagnostic.client.DiagnosticService;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class MultiDiagnosticServiceConnectionTest {
  @Mock
  private DiagnosticService diagnosticService;

  private MultiDiagnosticServiceProvider multiDiagnosticServiceProvider;

  private List<InetSocketAddress> nodes = asList(
      InetSocketAddress.createUnresolved("host1", 1234),
      InetSocketAddress.createUnresolved("host2", 1235),
      InetSocketAddress.createUnresolved("host1", 1235),
      InetSocketAddress.createUnresolved("host2", 1234));

  @Before
  public void setUp() {
    Duration timeout = Duration.ofSeconds(1);
    DiagnosticServiceProvider diagnosticServiceProvider = new DiagnosticServiceProvider("conn-name", timeout, timeout, null) {
      @Override
      public DiagnosticService fetchDiagnosticService(InetSocketAddress address, Duration timeout) {
        return diagnosticService;
      }
    };
    multiDiagnosticServiceProvider = new ConcurrentDiagnosticServiceProvider(diagnosticServiceProvider, timeout, new ConcurrencySizing());
  }

  @Test
  public void createsConnections() throws DiagnosticServiceProviderException {
    DiagnosticServices diagnosticServices = multiDiagnosticServiceProvider.fetchOnlineDiagnosticServices(nodes);
    for (InetSocketAddress address : nodes) {
      assertThat(diagnosticServices.getDiagnosticService(address).get(), is(sameInstance(diagnosticService)));
    }
  }

  @Test
  public void getEndpoints() throws DiagnosticServiceProviderException {
    DiagnosticServices diagnosticServices = multiDiagnosticServiceProvider.fetchOnlineDiagnosticServices(nodes);
    assertThat(diagnosticServices.getOnlineEndpoints(), containsInAnyOrder(nodes.toArray()));
  }

  @Test
  public void close() throws DiagnosticServiceProviderException {
    DiagnosticServices diagnosticServices = multiDiagnosticServiceProvider.fetchOnlineDiagnosticServices(nodes);
    diagnosticServices.close();
    verify(diagnosticService, times(4)).close();
  }

}
