/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.connection.entity.Entity;
import org.terracotta.entity.EndpointDelegate;
import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.entity.EntityUserException;
import org.terracotta.entity.Invocation;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Alex Snaps
 */
class VoltronProxyInvocationHandler implements InvocationHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(VoltronProxyInvocationHandler.class);

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
  private final ExecutorService handler;
  private final ConcurrentMap<Class<?>, CopyOnWriteArrayList<MessageListener<?>>> listeners;

  private volatile EndpointListener endpointListener;

  VoltronProxyInvocationHandler(final EntityClientEndpoint<ProxyEntityMessage, ProxyEntityResponse> entityClientEndpoint, Collection<Class<?>> events, final Codec codec) {
    this.entityClientEndpoint = entityClientEndpoint;
    String threadName = "Message Handler for " + entityClientEndpoint.toString();
    handler = Executors.newSingleThreadExecutor(r -> new Thread(r, threadName));
    this.listeners = new ConcurrentHashMap<>();
    if (!events.isEmpty()) {
      for (Class<?> aClass : events) {
        listeners.put(aClass, new CopyOnWriteArrayList<>());
      }

      entityClientEndpoint.setDelegate(new EndpointDelegate<ProxyEntityResponse>() {

        @SuppressWarnings({"unchecked", "rawtypes"})
        @Override
        public void handleMessage(ProxyEntityResponse response) {
          try {
            handler.execute(() -> {
              final Class<?> aClass = response.getResponseType();
              try {
                for (MessageListener messageListener : listeners.get(aClass)) {
                  messageListener.onMessage(response.getResponse());
                }
              } catch (Exception e) {
                LOGGER.warn("Error handling incoming server message {}: {}", aClass, e.getMessage(), e);
              }
            });
          } catch (RejectedExecutionException e) {
            // do nothing: this is normal in case the executor is closed,
            // and we can forget the message because the caller wants to close anyway
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
          handler.shutdownNow();
        }
      });
    }
  }

  @Override
  public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {

    if (close.equals(method)) {
      handler.shutdown();
      entityClientEndpoint.close();
      return null;

    } else if (registerMessageListener.equals(method)) {
      final Class<?> eventType = (Class<?>) args[0];
      final MessageListener<?> arg = (MessageListener<?>) args[1];
      final CopyOnWriteArrayList<MessageListener<?>> messageListeners = listeners.get(eventType);
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

    final Invocation<ProxyEntityResponse> builder = entityClientEndpoint.message(new ProxyEntityMessage(methodDescriptor, args, MessageType.MESSAGE));

    if (methodDescriptor.isAsync()) {
      return new ProxiedInvokeFuture<>(builder.invoke());
    } else {
      try {
        return getResponse(builder.invoke().get());
      } catch (ExecutionException e) {
        throw e.getCause();
      }
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

  private static class ProxiedInvokeFuture<T> implements Future<T> {

    private final Future<ProxyEntityResponse> future;

    public ProxiedInvokeFuture(final Future<ProxyEntityResponse> future) {
      this.future = future;
    }

    @Override
    public boolean cancel(final boolean mayInterruptIfRunning) {
      return future.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
      return future.isCancelled();
    }

    @Override
    public boolean isDone() {
      return future.isDone();
    }

    @SuppressWarnings("unchecked")
    @Override
    public T get() throws InterruptedException, ExecutionException {
      ProxyEntityResponse response = future.get();
      try {
        return (T) getResponse(response);
      } catch (EntityUserException e) {
        throw new ExecutionException(e);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public T get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
      ProxyEntityResponse response = future.get(timeout, unit);
      try {
        return (T) getResponse(response);
      } catch (EntityUserException e) {
        throw new ExecutionException(e);
      }
    }
  }
}
