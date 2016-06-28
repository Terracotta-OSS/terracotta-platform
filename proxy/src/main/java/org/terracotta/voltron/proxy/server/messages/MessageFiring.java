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
package org.terracotta.voltron.proxy.server.messages;

import org.terracotta.voltron.proxy.client.messages.MessageListener;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Alex Snaps
 */
public abstract class MessageFiring {

  private static final MessageListener FAKE = new MessageListener() {
    @Override
    public void onMessage(final Object message) {
      // no op;
    }
  };

  private final ConcurrentMap<Class<?>, MessageListener> listeners = new ConcurrentHashMap<Class<?>, MessageListener>();

  public MessageFiring(Class<?>... messageTypes) {
    for (Class<?> messageType : messageTypes) {
      this.listeners.put(messageType, FAKE);
    }
  }

  protected void fire(Object message) {
    listeners.get(message.getClass()).onMessage(message);
  }

  public <T> void registerListener(Class<T> messageType, MessageListener<T> listener) {
    if(!listeners.replace(messageType, FAKE, listener)) {
      throw new IllegalStateException();
    }
  }
}
