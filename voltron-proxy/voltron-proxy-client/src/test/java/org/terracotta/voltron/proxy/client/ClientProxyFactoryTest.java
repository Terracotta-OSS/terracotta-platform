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

import org.junit.Test;
import org.mockito.Matchers;
import org.terracotta.connection.entity.Entity;
import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.InvocationBuilder;
import org.terracotta.entity.InvokeFuture;
import org.terracotta.entity.MessageCodecException;
import org.terracotta.exception.EntityException;
import org.terracotta.voltron.proxy.Async;
import org.terracotta.voltron.proxy.MessageListener;
import org.terracotta.voltron.proxy.SerializationCodec;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.terracotta.voltron.proxy.ProxyEntityResponse.messageResponse;

/**
 * @author Alex Snaps
 */
public class ClientProxyFactoryTest {

  @Test
  public void addEntityInterfaceToType() {
    final SerializationCodec codec = new SerializationCodec();
    final EntityClientEndpoint clientEndpoint = mock(EntityClientEndpoint.class);
    final Comparable proxy = ClientProxyFactory.createProxy(Comparable.class, Comparable.class, clientEndpoint, null, codec);
    assertThat(proxy, instanceOf(Entity.class));
    ((Entity) proxy).close();
    verify(clientEndpoint).close();
  }

  @Test
  public void testFakeOutboundCall() throws ExecutionException, InterruptedException, EntityException, MessageCodecException {
    final SerializationCodec codec = new SerializationCodec();
    final EntityClientEndpoint endpoint = mock(EntityClientEndpoint.class);
    final InvocationBuilder builder = mock(InvocationBuilder.class);
    when(endpoint.beginInvoke()).thenReturn(builder);
    when(builder.message(Matchers.<EntityMessage>any())).thenReturn(builder);
    final InvokeFuture future = mock(InvokeFuture.class);
    when(builder.invoke()).thenReturn(future);
    when(future.get()).thenReturn(messageResponse(Integer.class, 42));

    final PassThrough proxy = ClientProxyFactory.createProxy(PassThrough.class, PassThrough.class, endpoint, null, codec);
    assertThat(proxy.sync(), is(42));
  }

  @Test
  public void testPassFutureThrough() throws ExecutionException, InterruptedException, TimeoutException, EntityException, MessageCodecException {
    final SerializationCodec codec = new SerializationCodec() {
      @Override
      public <T> T decode(final Class<T> type, final byte[] buffer) {
        assertThat(type == Integer.class, is(true));
        return super.decode(type, buffer);
      }
    };
    final EntityClientEndpoint endpoint = mock(EntityClientEndpoint.class);
    final InvocationBuilder builder = mock(InvocationBuilder.class);
    when(endpoint.beginInvoke()).thenReturn(builder);
    when(builder.message(Matchers.<EntityMessage>any())).thenReturn(builder);
    final InvokeFuture future = mock(InvokeFuture.class);
    when(builder.invoke()).thenReturn(future);
    when(future.get()).thenReturn(messageResponse(Integer.class, 42));
    when(future.getWithTimeout(1, TimeUnit.SECONDS)).thenReturn(messageResponse(Integer.class, 43))
        .thenThrow(new TimeoutException("Blah!"));

    final PassThrough proxy = ClientProxyFactory.createProxy(PassThrough.class, PassThrough.class, endpoint, null, codec);
    assertThat(proxy.aSync().get(), is(42));
    assertThat(proxy.aSync().get(1, TimeUnit.SECONDS), is(43));
    try {
      proxy.aSync().get(1, TimeUnit.SECONDS);
      fail();
    } catch (TimeoutException e) {
      assertThat(e.getMessage(), is("Blah!"));
    }
  }

  @Test
  public void testRegistersListeners() throws ExecutionException, InterruptedException, TimeoutException, EntityException, MessageCodecException {
    final SerializationCodec codec = new SerializationCodec();
    final EntityClientEndpoint endpoint = mock(EntityClientEndpoint.class);
    final InvocationBuilder builder = mock(InvocationBuilder.class);
    when(endpoint.beginInvoke()).thenReturn(builder);
    when(builder.message(Matchers.<EntityMessage>any())).thenReturn(builder);
    final InvokeFuture future = mock(InvokeFuture.class);
    when(builder.invoke()).thenReturn(future);
    when(future.get()).thenReturn(messageResponse(Integer.class, 42));

    final ListenerAware proxy = ClientProxyFactory.createEntityProxy(ListenerAware.class, PassThrough.class, endpoint, new Class<?>[]{String.class, Integer.class, Long.class, Double.class}, codec);
    assertThat(proxy.sync(), is(42));
    proxy.registerMessageListener(String.class, new StringMessageListener());
    proxy.registerMessageListener(Integer.class, new MessageListener<Integer>() {
      @Override
      public void onMessage(final Integer message) {
        throw new UnsupportedOperationException("Implement me!");
      }
    });
    proxy.registerMessageListener(Long.class, new ComplexMessageListener());
    proxy.registerMessageListener(Double.class, new MoreComplexMessageListener());
    try {
      proxy.registerMessageListener(Object.class, new MessageListener() {
        @Override
        public void onMessage(final Object message) {
          throw new UnsupportedOperationException("Implement me!");
        }
      });
      fail();
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

  public interface PassThrough {

    Integer sync();

    @Async
    Future<Integer> aSync();

  }

  public interface ListenerAware extends PassThrough, ServerMessageAware, Entity {

  }

  private static class StringMessageListener implements MessageListener<String> {
    public void onMessage(final String message) {
      throw new UnsupportedOperationException("Implement me!");
    }
  }
  
  private static class ComplexMessageListener implements ListenerSubInterface {

    public void onMessage(Long message) {
      throw new UnsupportedOperationException("Implement me!");
    }
    
  }
  
  private static interface ListenerSubInterface extends MessageListener<Long> {
  }

  private static class MoreComplexMessageListener implements ListenerGenericSubInterface<Double> {

    public void onMessage(Double message) {
      throw new UnsupportedOperationException("Implement me!");
    }
    
  }
  
  private static interface ListenerGenericSubInterface<T> extends MessageListener<T> {
  }
}