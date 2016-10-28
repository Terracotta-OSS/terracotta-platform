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
import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.entity.InvocationBuilder;
import org.terracotta.entity.InvokeFuture;
import org.terracotta.exception.EntityException;
import org.terracotta.voltron.proxy.Async;
import org.terracotta.voltron.proxy.MessageListener;
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

import static org.terracotta.voltron.proxy.Async.Ack.NONE;
import static org.terracotta.voltron.proxy.Async.Ack.RECEIVED;

/**
 * @author Alex Snaps
 */
class VoltronProxyInvocationHandler implements InvocationHandler {

  private static final Method close;
  private static final Method registerListener;

  static {
    try {
      close = Entity.class.getDeclaredMethod("close");
      registerListener = ServerMessageAware.class.getDeclaredMethod("registerListener", MessageListener.class);
    } catch (NoSuchMethodException e) {
      throw new AssertionError("Someone changed some method signature here!!!");
    }
  }

  private final EntityClientEndpoint<ProxyEntityMessage, ProxyEntityResponse> entityClientEndpoint;
  private final ConcurrentMap<Class<?>, CopyOnWriteArrayList<MessageListener>> listeners;

  public VoltronProxyInvocationHandler(final EntityClientEndpoint<ProxyEntityMessage, ProxyEntityResponse> entityClientEndpoint, Collection<Class<?>> events) {
    this.entityClientEndpoint = entityClientEndpoint;
    this.listeners = new ConcurrentHashMap<Class<?>, CopyOnWriteArrayList<MessageListener>>();
    if (events.size() > 0) {
      for (Class<?> aClass : events) {
        listeners.put(aClass, new CopyOnWriteArrayList<MessageListener>());
      }
      entityClientEndpoint.setDelegate(new ProxyEndpointDelegate(listeners));
    }
  }

  @Override
  public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {

    if(close.equals(method)) {
      entityClientEndpoint.close();
      return null;
    } else if(registerListener.equals(method)) {
      final MessageListener arg = (MessageListener) args[0];
      Class<?> eventType = getMessageListenerEventType(arg);
      final CopyOnWriteArrayList<MessageListener> messageListeners = listeners.get(eventType);
      if(messageListeners == null) {
        throw new IllegalArgumentException("Event type '" + eventType + "' isn't supported");
      }
      messageListeners.add(arg);
      return null;
    }

    final MethodDescriptor methodDescriptor = MethodDescriptor.of(method);

    final InvocationBuilder<ProxyEntityMessage, ProxyEntityResponse> builder = entityClientEndpoint.beginInvoke()
            .message(new ProxyEntityMessage(methodDescriptor, args));

    if(methodDescriptor.isAsync()) {
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
      return builder.invoke().get().getResponse();
    }
  }

  private static Class<?> getMessageListenerEventType(MessageListener from) {
    for (Method m: from.getClass().getMethods()) {
      if (m.getName().equals("onMessage") && !m.isBridge()) {
        Class<?>[] params = m.getParameterTypes();
        if (params.length == 1 && !m.getParameterTypes()[0].isPrimitive()) {
          return m.getParameterTypes()[0];
        }
      }
    }
    throw new AssertionError();
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
        return future.get().getResponse();
      } catch (EntityException e) {
        throw new ExecutionException(e);
      }
    }

    @Override
    public Object get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
      try {
        return future.getWithTimeout(timeout, unit).getResponse();
      } catch (EntityException e) {
        throw new ExecutionException(e);
      }
    }
  }
}
