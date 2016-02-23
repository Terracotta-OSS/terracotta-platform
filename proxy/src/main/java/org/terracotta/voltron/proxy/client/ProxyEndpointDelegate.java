/*
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Connection API.
 *
 * The Initial Developer of the Covered Software is
 * Terracotta, Inc., a Software AG company
 */

package org.terracotta.voltron.proxy.client;

import org.terracotta.entity.EndpointDelegate;
import org.terracotta.voltron.proxy.Codec;
import org.terracotta.voltron.proxy.client.messages.MessageListener;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Alex Snaps
 */
class ProxyEndpointDelegate implements EndpointDelegate {

  private final Codec codec;
  private final ConcurrentMap<Class<?>, CopyOnWriteArrayList<MessageListener>> listeners;
  private final Map<Byte, Class<?>> eventMappings;

  public ProxyEndpointDelegate(final Codec codec,
                               final ConcurrentMap<Class<?>, CopyOnWriteArrayList<MessageListener>> listeners,
                               final Map<Byte, Class<?>> eventMappings) {
    this.codec = codec;
    this.listeners = listeners;
    this.eventMappings = eventMappings;
  }

  @Override
  public void handleMessage(final byte[] bytes) {
    final Object message = codec.decode(Arrays.copyOfRange(bytes, 1, bytes.length), eventMappings.get(bytes[0]));
    final Class<?> aClass = message.getClass();
    for (MessageListener messageListener : listeners.get(aClass)) {
      messageListener.onMessage(message);
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
