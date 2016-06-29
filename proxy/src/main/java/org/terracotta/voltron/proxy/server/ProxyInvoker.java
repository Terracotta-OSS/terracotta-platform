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
package org.terracotta.voltron.proxy.server;

import org.terracotta.entity.ClientCommunicator;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.MessageCodecException;
import org.terracotta.voltron.proxy.client.messages.MessageListener;
import org.terracotta.voltron.proxy.server.messages.MessageFiring;
import org.terracotta.voltron.proxy.server.messages.ProxyEntityMessage;
import org.terracotta.voltron.proxy.server.messages.ProxyEntityResponse;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author Alex Snaps
 */
public class ProxyInvoker<T> {

  private final T target;
  private final Set<Class<?>> messageTypes;
  private final ClientCommunicator clientCommunicator;
  private final Set<ClientDescriptor> clients = Collections.synchronizedSet(new HashSet<ClientDescriptor>());

  private final ThreadLocal<InvocationContext> invocationContext = new ThreadLocal<InvocationContext>();

  public ProxyInvoker(T target) {
    this(target, null);
  }

  public ProxyInvoker(T target, ClientCommunicator clientCommunicator, Class<?> ... messageTypes) {
    this.target = target;
    this.messageTypes = new HashSet<Class<?>>();
    for (Class eventType : messageTypes) {
      this.messageTypes.add(eventType);
      if(target instanceof MessageFiring) {
        ((MessageFiring)target).registerListener(eventType, new MessageListener() {
          @Override
          public void onMessage(final Object message) {
            fireMessage(message);
          }
        });
      }
    }
    if (messageTypes.length != 0 && clientCommunicator == null) {
      throw new IllegalArgumentException("Messages cannot be sent using a null ClientCommunicator");
    } else {
      this.clientCommunicator = clientCommunicator;
    }
  }

  public ProxyEntityResponse invoke(final ClientDescriptor clientDescriptor, final ProxyEntityMessage message) {
    try {
      try {
        invocationContext.set(new InvocationContext(clientDescriptor));
        return ProxyEntityResponse.response(message.messageType(), message.invoke(target, clientDescriptor));
      } finally {
        invocationContext.remove();
      }
    } catch (IllegalAccessException e) {
      throw new IllegalArgumentException(e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e.getCause());
    }
  }

  public void fireMessage(Object message) {
    final Class<?> type = message.getClass();
    if(!messageTypes.contains(type)) {
      throw new IllegalArgumentException("Event type '" + type + "' isn't supported");
    }
    Set<Future<Void>> futures = new HashSet<Future<Void>>();
    final InvocationContext invocationContext = this.invocationContext.get();
    final ClientDescriptor caller = invocationContext == null ? null : invocationContext.caller;
    for (ClientDescriptor client : clients) {
      if (!client.equals(caller)) {
        try {
          futures.add(clientCommunicator.send(client, ProxyEntityResponse.response(type, message)));
        } catch (MessageCodecException ex) {
          throw new RuntimeException(ex);
        }
      }
    }
    boolean interrupted = false;
    while(!futures.isEmpty()) {
      for (Iterator<Future<Void>> iterator = futures.iterator(); iterator.hasNext(); ) {
        final Future<Void> future = iterator.next();
        try {
          future.get();
          iterator.remove();
        } catch (InterruptedException e) {
          interrupted = true;
        } catch (ExecutionException e) {
          iterator.remove();
          e.printStackTrace();
        }
      }
    }
    if(interrupted) {
      Thread.currentThread().interrupt();
    }
  }

  public void fireAndForgetMessage(Object message, ClientDescriptor... clients) {
    final Class<?> type = message.getClass();
    if(!messageTypes.contains(type)) {
      throw new IllegalArgumentException("Event type '" + type + "' isn't supported");
    }
    for (ClientDescriptor client : clients) {
      try {
        clientCommunicator.sendNoResponse(client, ProxyEntityResponse.response(type, message));
      } catch (MessageCodecException ex) {
        throw new RuntimeException(ex);
      }
    }
  }

  public void addClient(ClientDescriptor descriptor) {
    clients.add(descriptor);
  }

  public void removeClient(ClientDescriptor descriptor) {
    clients.remove(descriptor);
  }

  private final class InvocationContext {

    private final ClientDescriptor caller;

    public InvocationContext(final ClientDescriptor caller) {
      this.caller = caller;
    }
  }
}
