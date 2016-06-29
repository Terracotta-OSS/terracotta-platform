/**
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
package org.terracotta.voltron.proxy.client;

import org.terracotta.entity.EndpointDelegate;
import org.terracotta.entity.EntityResponse;
import org.terracotta.voltron.proxy.client.messages.MessageListener;
import org.terracotta.voltron.proxy.server.messages.ProxyEntityResponse;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Alex Snaps
 */
class ProxyEndpointDelegate implements EndpointDelegate {

  private final ConcurrentMap<Class<?>, CopyOnWriteArrayList<MessageListener>> listeners;

  public ProxyEndpointDelegate(final ConcurrentMap<Class<?>, CopyOnWriteArrayList<MessageListener>> listeners) {
    this.listeners = listeners;
  }

  @Override
  public void handleMessage(EntityResponse messageFromServer) {
    ProxyEntityResponse response = (ProxyEntityResponse)messageFromServer;
    final Class<?> aClass = response.getResponseType();
    for (MessageListener messageListener : listeners.get(aClass)) {
      messageListener.onMessage(response.getResponse());
    }
  }

  @Override
  public byte[] createExtendedReconnectData() {
    // no idea?!
    return new byte[0];
  }

  @Override
  public void didDisconnectUnexpectedly() {
    // no idea?!
  }
}
