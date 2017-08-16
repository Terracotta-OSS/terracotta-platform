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
package org.terracotta.voltron.proxy.client;

import org.terracotta.connection.entity.Entity;
import org.terracotta.entity.EndpointDelegate;
import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.InvocationBuilder;
import org.terracotta.entity.InvokeFuture;
import org.terracotta.exception.EntityException;
import org.terracotta.entity.EntityUserException;
import org.terracotta.voltron.proxy.Codec;
import org.terracotta.voltron.proxy.MessageListener;
import org.terracotta.voltron.proxy.MessageType;
import org.terracotta.voltron.proxy.MethodDescriptor;
import org.terracotta.voltron.proxy.ProxyEntityMessage;
import org.terracotta.voltron.proxy.ProxyEntityResponse;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Alex Snaps
 */
class VoltronProxyInvocationHandler implements InvocationHandler {

  private static final Method close;
  private static final Method registerMessageListener;
  private static final Method setEndpointListener;

  static {
    try {
      close = Entity.class.getDeclaredMethod("close");
      registerMessageListener = ServerMessageAware.class.getDeclaredMethod("registerMessageListener", Class.class, MessageListener.class);
      setEndpointListener = EndpointListenerAware.class.getDeclaredMethod("setEndpointListener", EndpointListener.class);
    } catch (NoSuchMethodException e) {
      throw new AssertionError("Someone changed some method signature here!!!");
    }
  }

  private final EntityClientEndpoint<ProxyEntityMessage, ProxyEntityResponse> entityClientEndpoint;
  private final ConcurrentMap<Class<?>, CopyOnWriteArrayList<MessageListener>> listeners;

  private volatile EndpointListener endpointListener;

  VoltronProxyInvocationHandler(final EntityClientEndpoint<ProxyEntityMessage, ProxyEntityResponse> entityClientEndpoint, Collection<Class<?>> events, final Codec codec) {
    this.entityClientEndpoint = entityClientEndpoint;
    this.listeners = new ConcurrentHashMap<Class<?>, CopyOnWriteArrayList<MessageListener>>();
    if (events.size() > 0) {
      for (Class<?> aClass : events) {
        listeners.put(aClass, new CopyOnWriteArrayList<MessageListener>());
      }

      entityClientEndpoint.setDelegate(new EndpointDelegate<ProxyEntityResponse>() {

        @SuppressWarnings("unchecked")
        @Override
        public void handleMessage(ProxyEntityResponse response) {
          final Class<?> aClass = response.getResponseType();
          for (MessageListener messageListener : listeners.get(aClass)) {
            messageListener.onMessage(response.getResponse());
          }
        }

        @Override
        public byte[] createExtendedReconnectData() {
          if (endpointListener == null) {
            return null;
          } else {
            Object state = endpointListener.onReconnect();
            if (state == null) {
              return null;
            }
            return codec.encode(state.getClass(), state);
          }
        }

        @Override
        public void didDisconnectUnexpectedly() {
          if (endpointListener != null) {
            endpointListener.onDisconnectUnexpectedly();
          }
        }
      });
    }
  }

  @Override
  public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {

    if (close.equals(method)) {
      entityClientEndpoint.close();
      return null;

    } else if (registerMessageListener.equals(method)) {
      final Class<?> eventType = (Class<?>) args[0];
      final MessageListener arg = (MessageListener) args[1];
      final CopyOnWriteArrayList<MessageListener> messageListeners = listeners.get(eventType);
      if (messageListeners == null) {
        throw new IllegalArgumentException("Event type '" + eventType + "' isn't supported");
      }
      messageListeners.add(arg);
      return null;

    } else if (setEndpointListener.equals(method)) {
      this.endpointListener = (EndpointListener) args[0];
      return null;
    }

    final MethodDescriptor methodDescriptor = MethodDescriptor.of(method);

    final InvocationBuilder<ProxyEntityMessage, ProxyEntityResponse> builder = entityClientEndpoint.beginInvoke()
        .message(new ProxyEntityMessage(methodDescriptor, args, MessageType.MESSAGE));

    if (methodDescriptor.isAsync()) {
      switch (methodDescriptor.getAck()) {
        case NONE:
          break;
        case RECEIVED:
          builder.ackReceived();
          break;
        default:
          throw new IllegalStateException(methodDescriptor.getAck().name());
      }
      return new ProxiedInvokeFuture(builder.invoke());

    } else {
      return getResponse(builder.invoke().get());
    }
  }

  private static Object getResponse(ProxyEntityResponse proxyEntityResponse) throws EntityUserException {
    if (proxyEntityResponse == null) {
      return null;
    }
    if (proxyEntityResponse.getMessageType() == MessageType.ERROR) {
      throw (EntityUserException) proxyEntityResponse.getResponse();
    }
    return proxyEntityResponse.getResponse();
  }

  private static class ProxiedInvokeFuture implements Future {

    private final InvokeFuture<ProxyEntityResponse> future;

    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    public ProxiedInvokeFuture(final InvokeFuture<ProxyEntityResponse> future) {
      this.future = future;
    }

    @Override
    public boolean cancel(final boolean mayInterruptIfRunning) {
      if (cancelled.compareAndSet(false, true)) {
        future.interrupt();
        return true;
      }
      return false;
    }

    @Override
    public boolean isCancelled() {
      return cancelled.get();
    }

    @Override
    public boolean isDone() {
      return future.isDone();
    }

    @Override
    public Object get() throws InterruptedException, ExecutionException {
      try {
        return getResponse(future.get());
      } catch (EntityException e) {
        throw new ExecutionException(e);
      } catch (EntityUserException e) {
        throw new ExecutionException(e);
      }
    }

    @Override
    public Object get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
      try {
        return getResponse(future.getWithTimeout(timeout, unit));
      } catch (EntityException e) {
        throw new ExecutionException(e);
      } catch (EntityUserException e) {
        throw new ExecutionException(e);
      }
    }
  }
}
