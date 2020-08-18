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
package org.terracotta.diagnostic.client.connection;

import java.net.InetSocketAddress;
import java.time.Duration;

import org.junit.Test;
import org.mockito.Mock;
import org.terracotta.diagnostic.client.DiagnosticService;
import org.terracotta.json.ObjectMapperFactory;

import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * DiagnosticServiceProviderTest
 */
public class DiagnosticServiceProviderTest {

  @Mock
  private DiagnosticService diagnosticService;

  @Test
  public void testFallBackWhenSecurityRootDirectoryNotProvided() {
    InetSocketAddress node = InetSocketAddress.createUnresolved("host", 1234);

    Duration timeout = Duration.ofSeconds(1);
    DiagnosticServiceProvider diagnosticServiceProvider = spy(new DiagnosticServiceProvider("test", timeout, timeout, null, new ObjectMapperFactory()));

    doThrow(new DiagnosticServiceProviderException("failed")).when(diagnosticServiceProvider).fetchDiagnosticService(node);

    try {
      diagnosticServiceProvider.fetchDiagnosticServiceWithFallback(node);
    } catch (DiagnosticServiceProviderException ignored) {
    }

    verify(diagnosticServiceProvider, times(1)).fetchDiagnosticService(node);
    verify(diagnosticServiceProvider, times(0)).fetchUnSecuredDiagnosticService(node);
  }

  @Test
  public void testFallBackWhenSecurityRootDirectoryProvided() {
    InetSocketAddress node = InetSocketAddress.createUnresolved("host", 1234);

    Duration timeout = Duration.ofSeconds(1);
    DiagnosticServiceProvider diagnosticServiceProvider = spy(new DiagnosticServiceProvider("test", timeout, timeout, "./security", new ObjectMapperFactory()));

    doThrow(new DiagnosticServiceProviderException("failed")).when(diagnosticServiceProvider).fetchDiagnosticService(node);
    doReturn(diagnosticService).when(diagnosticServiceProvider).fetchUnSecuredDiagnosticService(node);

    assertThat(diagnosticServiceProvider.fetchDiagnosticServiceWithFallback(node), is(sameInstance(diagnosticService)));

    verify(diagnosticServiceProvider, times(1)).fetchDiagnosticService(node);
    verify(diagnosticServiceProvider, times(1)).fetchUnSecuredDiagnosticService(node);
  }
}
