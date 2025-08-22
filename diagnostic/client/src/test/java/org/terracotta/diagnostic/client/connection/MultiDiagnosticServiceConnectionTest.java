/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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
package org.terracotta.diagnostic.client.connection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.terracotta.diagnostic.client.DiagnosticService;
import org.terracotta.json.DefaultJsonFactory;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class MultiDiagnosticServiceConnectionTest {
  @Mock
  private DiagnosticService diagnosticService;

  private MultiDiagnosticServiceProvider multiDiagnosticServiceProvider;

  private Map<InetSocketAddress, InetSocketAddress> nodes = Stream.of(
      InetSocketAddress.createUnresolved("host1", 1234),
      InetSocketAddress.createUnresolved("host2", 1235),
      InetSocketAddress.createUnresolved("host1", 1235),
      InetSocketAddress.createUnresolved("host2", 1234)).collect(toMap(identity(), identity()));

  @Before
  public void setUp() {
    Duration timeout = Duration.ofSeconds(1);
    DiagnosticServiceProvider diagnosticServiceProvider = new DefaultDiagnosticServiceProvider("conn-name", timeout, timeout, null, new DefaultJsonFactory()) {
      @Override
      public DiagnosticService fetchDiagnosticService(InetSocketAddress address, Duration timeout) {
        return diagnosticService;
      }
    };
    multiDiagnosticServiceProvider = new ConcurrentDiagnosticServiceProvider(diagnosticServiceProvider, timeout, new ConcurrencySizing());
  }

  @Test
  public void createsConnections() throws DiagnosticServiceProviderException {
    DiagnosticServices<InetSocketAddress> diagnosticServices = multiDiagnosticServiceProvider.fetchOnlineDiagnosticServices(nodes);
    for (InetSocketAddress address : nodes.keySet()) {
      assertThat(diagnosticServices.getDiagnosticService(address).get(), is(sameInstance(diagnosticService)));
    }
  }

  @Test
  public void getEndpoints() throws DiagnosticServiceProviderException {
    DiagnosticServices<InetSocketAddress> diagnosticServices = multiDiagnosticServiceProvider.fetchOnlineDiagnosticServices(nodes);
    assertThat(diagnosticServices.getOnlineEndpoints().keySet(), is(equalTo(nodes.keySet())));
  }

  @Test
  public void close() throws DiagnosticServiceProviderException {
    DiagnosticServices<InetSocketAddress> diagnosticServices = multiDiagnosticServiceProvider.fetchOnlineDiagnosticServices(nodes);
    diagnosticServices.close();
    verify(diagnosticService, times(4)).close();
  }

}
