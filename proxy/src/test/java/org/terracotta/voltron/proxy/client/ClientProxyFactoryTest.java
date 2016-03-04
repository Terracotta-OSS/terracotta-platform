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

import org.junit.Test;
import org.mockito.Matchers;
import org.terracotta.connection.entity.Entity;
import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.entity.InvocationBuilder;
import org.terracotta.entity.InvokeFuture;
import org.terracotta.exception.EntityException;
import org.terracotta.voltron.proxy.ProxyMessageCodec;
import org.terracotta.voltron.proxy.SerializationCodec;
import org.terracotta.voltron.proxy.client.messages.MessageListener;
import org.terracotta.voltron.proxy.client.messages.ServerMessageAware;

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
import static org.terracotta.voltron.proxy.server.messages.ProxyEntityResponse.response;

/**
 * @author Alex Snaps
 */
public class ClientProxyFactoryTest {

  @Test
  public void addEntityInterfaceToType() {
    final EntityClientEndpoint clientEndpoint = mock(EntityClientEndpoint.class);
    final Comparable proxy = ClientProxyFactory.createProxy(Comparable.class, Comparable.class, clientEndpoint);
    assertThat(proxy, instanceOf(Entity.class));
    ((Entity) proxy).close();
    verify(clientEndpoint).close();
  }

  @Test
  public void testFakeOutboundCall() throws ExecutionException, InterruptedException, EntityException {
    final SerializationCodec codec = new SerializationCodec();
    final ProxyMessageCodec messageCodec = new ProxyMessageCodec(codec, PassThrough.class);
    final EntityClientEndpoint endpoint = mock(EntityClientEndpoint.class);
    final InvocationBuilder builder = mock(InvocationBuilder.class);
    when(endpoint.beginInvoke()).thenReturn(builder);
    when(builder.payload(Matchers.<byte[]>any())).thenReturn(builder);
    final InvokeFuture future = mock(InvokeFuture.class);
    when(builder.invoke()).thenReturn(future);
    when(future.get()).thenReturn(messageCodec.serialize(response(Integer.class, 42)));

    final PassThrough proxy = ClientProxyFactory.createProxy(PassThrough.class, PassThrough.class, endpoint, codec);
    assertThat(proxy.sync(), is(42));
  }

  @Test
  public void testPassFutureThrough() throws ExecutionException, InterruptedException, TimeoutException, EntityException {
    final SerializationCodec codec = new SerializationCodec() {
      @Override
      public Object decode(final byte[] buffer, final Class<?> type) {
        assertThat(type == Integer.class, is(true));
        return super.decode(buffer, type);
      }
    };
    final ProxyMessageCodec messageCodec = new ProxyMessageCodec(codec, PassThrough.class);
    final EntityClientEndpoint endpoint = mock(EntityClientEndpoint.class);
    final InvocationBuilder builder = mock(InvocationBuilder.class);
    when(endpoint.beginInvoke()).thenReturn(builder);
    when(builder.payload(Matchers.<byte[]>any())).thenReturn(builder);
    final InvokeFuture future = mock(InvokeFuture.class);
    when(builder.invoke()).thenReturn(future);
    when(future.get()).thenReturn(messageCodec.serialize(response(Integer.class, 42)));
    when(future.getWithTimeout(1, TimeUnit.SECONDS)).thenReturn(codec.encode(Integer.class, 43))
        .thenThrow(new TimeoutException("Blah!"));

    final PassThrough proxy = ClientProxyFactory.createProxy(PassThrough.class, PassThrough.class, endpoint, codec);
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
  public void testRegistersListeners() throws ExecutionException, InterruptedException, TimeoutException, EntityException {
    final SerializationCodec codec = new SerializationCodec();
    final ProxyMessageCodec messageCodec = new ProxyMessageCodec(codec, PassThrough.class);
    final EntityClientEndpoint endpoint = mock(EntityClientEndpoint.class);
    final InvocationBuilder builder = mock(InvocationBuilder.class);
    when(endpoint.beginInvoke()).thenReturn(builder);
    when(builder.payload(Matchers.<byte[]>any())).thenReturn(builder);
    final InvokeFuture future = mock(InvokeFuture.class);
    when(builder.invoke()).thenReturn(future);
    when(future.get()).thenReturn(messageCodec.serialize(response(Integer.class, 42)));

    final ListenerAware proxy = ClientProxyFactory.createEntityProxy(ListenerAware.class, PassThrough.class, endpoint, codec, String.class, Integer.class, Long.class, Double.class);
    assertThat(proxy.sync(), is(42));
    proxy.registerListener(new StringMessageListener());
    proxy.registerListener(new MessageListener<Integer>() {
      @Override
      public void onMessage(final Integer message) {
        throw new UnsupportedOperationException("Implement me!");
      }
    });
    proxy.registerListener(new ComplexMessageListener());
    proxy.registerListener(new MoreComplexMessageListener());
    try {
      proxy.registerListener(new MessageListener<Object>() {
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