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
package org.terracotta.lease.service.closer;

import org.junit.Test;
import org.terracotta.entity.ClientDescriptor;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ProxyClientConnectionCloserTest {
  @Test(expected = IllegalStateException.class)
  public void mustSetDelegateBeforeUse() {
    ClientDescriptor clientDescriptor = mock(ClientDescriptor.class);

    ProxyClientConnectionCloser proxyCloser = new ProxyClientConnectionCloser();
    proxyCloser.closeClientConnection(clientDescriptor);
  }

  @Test
  public void delegates() {
    ClientDescriptor clientDescriptor = mock(ClientDescriptor.class);
    ClientConnectionCloser delegateCloser = mock(ClientConnectionCloser.class);

    ProxyClientConnectionCloser proxyCloser = new ProxyClientConnectionCloser();
    proxyCloser.setClientConnectionCloser(delegateCloser);
    proxyCloser.closeClientConnection(clientDescriptor);

    verify(delegateCloser).closeClientConnection(clientDescriptor);
  }
}
