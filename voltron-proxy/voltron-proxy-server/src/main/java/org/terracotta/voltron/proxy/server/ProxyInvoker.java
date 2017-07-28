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
package org.terracotta.voltron.proxy.server;

import org.terracotta.entity.ActiveInvokeContext;
import org.terracotta.entity.ClientCommunicator;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.EntityUserException;
import org.terracotta.entity.InvokeContext;
import org.terracotta.entity.MessageCodecException;
import org.terracotta.voltron.proxy.ProxyEntityMessage;
import org.terracotta.voltron.proxy.ProxyEntityResponse;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Alex Snaps
 */
class ProxyInvoker<T> implements MessageFiring {

  private final T target;
  private final Set<ClientDescriptor> clients = Collections.synchronizedSet(new HashSet<ClientDescriptor>());
  private final ThreadLocal<InvocationContext> invocationContext = new ThreadLocal<>();

  private Set<Class<?>> messageTypes;
  private ClientCommunicator clientCommunicator;

  ProxyInvoker(T target) {
    this.target = target;
  }

  ProxyEntityResponse invoke(ActiveInvokeContext context, final ProxyEntityMessage message) {
    ClientDescriptor clientDescriptor = context.getClientDescriptor();
    try {
      invocationContext.set(new InvocationContext(clientDescriptor));
      return ProxyEntityResponse.response(message.getType(), message.messageType(), message.invoke(target, clientDescriptor));
    } catch (IllegalAccessException e) {
      throw new IllegalArgumentException(e);
    } catch (InvocationTargetException e) {
      Throwable targetException = e.getTargetException();
      if (targetException instanceof Error) {
        throw (Error) targetException;
      }
      StringBuilder errorMessage = new StringBuilder("Entity: ").append(target.getClass().getName())
          .append(": exception in user code: ")
          .append(targetException.getClass().getName())
          .append(": ")
          .append(targetException.getMessage());
      EntityUserException entityUserException = new EntityUserException(errorMessage.toString(), targetException);
      return ProxyEntityResponse.error(entityUserException);
    } finally {
      invocationContext.remove();
    }
  }

  void invoke(final ProxyEntityMessage message) {
    try {
      message.invoke(target);
    } catch (IllegalAccessException e) {
      throw new IllegalArgumentException(e);
    } catch (InvocationTargetException e) {
      Throwable target = e.getTargetException();
      if (target instanceof Error) {
        throw (Error) target;
      }
      if (target instanceof RuntimeException) {
        throw (RuntimeException) target;
      }
      throw new RuntimeException(target.getMessage(), target);
    }
  }

  @Override
  public <T> void fireMessage(Class<T> type, T message, boolean echo) {
    if (!messageTypes.contains(type)) {
      throw new IllegalArgumentException("Event type '" + type + "' isn't supported");
    }
    final InvocationContext invocationContext = this.invocationContext.get();
    final ClientDescriptor caller = invocationContext == null ? null : invocationContext.caller;
    for (ClientDescriptor client : clients) {
      if (echo || !client.equals(caller)) {
        try {
          clientCommunicator.sendNoResponse(client, ProxyEntityResponse.messageResponse(type, message));
        } catch (MessageCodecException ex) {
          handleExceptionOnSend(ex);
        }
      }
    }
  }

  @Override
  public <T> void fireMessage(Class<T> type, T message, ClientDescriptor[] clients) {
    if (!messageTypes.contains(type)) {
      throw new IllegalArgumentException("Event type '" + type + "' isn't supported");
    }
    for (ClientDescriptor client : clients) {
      try {
        clientCommunicator.sendNoResponse(client, ProxyEntityResponse.messageResponse(type, message));
      } catch (MessageCodecException ex) {
        handleExceptionOnSend(ex);
      }
    }
  }

  void addClient(ClientDescriptor descriptor) {
    clients.add(descriptor);
  }

  void removeClient(ClientDescriptor descriptor) {
    clients.remove(descriptor);
  }

  public Collection<ClientDescriptor> getClients() {
    return new ArrayList<>(clients);
  }

  private void handleExceptionOnSend(MessageCodecException ex) {
    throw new RuntimeException(ex);
  }

  ProxyInvoker<T> activateEvents(ClientCommunicator clientCommunicator, Class<?>[] messageTypes) {
    if (messageTypes == null) {
      messageTypes = new Class[0];
    }
    this.messageTypes = new HashSet<>(Arrays.asList(messageTypes));
    if (target instanceof MessageFiringSupport) {
      ((MessageFiringSupport) target).setMessageFiring(this);
    }

    for (Class eventType : messageTypes) {
      this.messageTypes.add(eventType);

    }
    if (messageTypes.length != 0 && clientCommunicator == null) {
      throw new IllegalArgumentException("Messages cannot be sent using a null ClientCommunicator");
    } else {
      this.clientCommunicator = clientCommunicator;
    }
    return this;
  }

  private static final class InvocationContext {

    private final ClientDescriptor caller;

    public InvocationContext(final ClientDescriptor caller) {
      this.caller = caller;
    }
  }
}
