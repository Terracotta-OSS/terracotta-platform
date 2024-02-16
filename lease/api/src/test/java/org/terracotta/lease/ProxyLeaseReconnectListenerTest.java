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
package org.terracotta.lease;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class ProxyLeaseReconnectListenerTest {
  @Mock
  private LeaseReconnectListener underlying;

  @Test
  public void noErrorWhenNoUnderlyingListener() {
    ProxyLeaseReconnectListener proxy = new ProxyLeaseReconnectListener();
    proxy.reconnected();
    proxy.reconnecting();
  }

  @Test
  public void delegatesReconnectingToUnderlyingListener() {
    ProxyLeaseReconnectListener proxy = new ProxyLeaseReconnectListener();
    proxy.setUnderlying(underlying);
    proxy.reconnecting();
    verify(underlying).reconnecting();
    verifyNoMoreInteractions(underlying);
  }

  @Test
  public void delegatesReconnectedToUnderlyingListener() {
    ProxyLeaseReconnectListener proxy = new ProxyLeaseReconnectListener();
    proxy.setUnderlying(underlying);
    proxy.reconnected();
    verify(underlying).reconnected();
    verifyNoMoreInteractions(underlying);
  }
}
