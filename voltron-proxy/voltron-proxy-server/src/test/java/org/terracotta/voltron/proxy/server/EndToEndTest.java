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
package org.terracotta.voltron.proxy.server;

import org.junit.Test;
import org.terracotta.connection.entity.Entity;
import org.terracotta.entity.ActiveInvokeContext;
import org.terracotta.entity.ClientCommunicator;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.ClientSourceId;
import org.terracotta.entity.EndpointDelegate;
import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.Invocation;
import org.terracotta.entity.InvocationCallback;
import org.terracotta.entity.MessageCodec;
import org.terracotta.voltron.proxy.ClientId;
import org.terracotta.voltron.proxy.MessageListener;
import org.terracotta.voltron.proxy.ProxyEntityMessage;
import org.terracotta.voltron.proxy.ProxyEntityResponse;
import org.terracotta.voltron.proxy.ProxyMessageCodec;
import org.terracotta.voltron.proxy.SerializationCodec;
import org.terracotta.voltron.proxy.client.ClientProxyFactory;
import org.terracotta.voltron.proxy.client.ServerMessageAware;

import java.io.Serializable;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.terracotta.entity.ActiveInvokeChannel;

/**
 * @author Alex Snaps
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class EndToEndTest {

  @SuppressWarnings("rawtypes")
  @Test
  public void testBothEnds() throws ExecutionException, InterruptedException {
    final SerializationCodec codec = new SerializationCodec();
    final ProxyMessageCodec messageCodec = new ProxyMessageCodec(Comparable.class, null);
    final ProxyInvoker<Comparable> proxyInvoker = new ProxyInvoker<Comparable>(new Comparable() {
      public int compareTo(final Object o) {
        return 42;
      }
    });
    final EntityClientEndpoint endpoint = mock(EntityClientEndpoint.class);
    when(endpoint.message(any())).thenAnswer(invocation -> new RecordingInvocation(proxyInvoker, messageCodec, invocation.getArgument(0)));

    final Comparable proxy = ClientProxyFactory.createProxy(Comparable.class, Comparable.class, endpoint, null, codec);
    assertThat(proxy.compareTo("blah!"), is(42));
  }

  @Test
  public void testServerInitiatedMessageFiring() throws ExecutionException, InterruptedException, TimeoutException {
    final SerializationCodec codec = new SerializationCodec();
    final AtomicReference<EndpointDelegate> delegate = new AtomicReference<EndpointDelegate>();
    final ProxyMessageCodec messageCodec = new ProxyMessageCodec(Comparable.class, new Class[] {String.class});
    final ProxyInvoker<Comparable> proxyInvoker = new ProxyInvoker<Comparable>(new Comparable() {
      public int compareTo(final Object o) {
        return 42;
      }
    }).activateEvents(new ClientCommunicator() {
      @Override
      public void closeClientConnection(ClientDescriptor clientDescriptor) {

      }
      public void sendNoResponse(final ClientDescriptor clientDescriptor, final EntityResponse message) {
        delegate.get().handleMessage(message);
      }

      public Future<Void> send(final ClientDescriptor clientDescriptor, final EntityResponse message) {
        throw new UnsupportedOperationException("Implement me!");
      }
    }, new Class[] {String.class});
    final EntityClientEndpoint endpoint = new EntityClientEndpoint<ProxyEntityMessage, ProxyEntityResponse>() {
      public byte[] getEntityConfiguration() {
        throw new UnsupportedOperationException("Implement me!");
      }

      public void setDelegate(final EndpointDelegate endpointDelegate) {
        delegate.set(endpointDelegate);
      }

      public Invocation message(ProxyEntityMessage message) {
        return new RecordingInvocation(proxyInvoker, messageCodec, message);
      }

      public byte[] getExtendedReconnectData() {
        throw new UnsupportedOperationException("Implement me!");
      }

      public void close() {
        throw new UnsupportedOperationException("Implement me!");
      }

      @Override
      public Future<Void> release() {
        throw new UnsupportedOperationException("TODO Implement me!");
      }

      public void didCloseUnexpectedly() {
        throw new UnsupportedOperationException("Implement me!");
      }
    };

    final ComparableEntity proxy = ClientProxyFactory.createEntityProxy(ComparableEntity.class, Comparable.class, endpoint, new Class[]{String.class}, codec);
    final CompletableFuture<String> messageReceived = new CompletableFuture<>();
    proxy.registerMessageListener(String.class, new MessageListener<String>() {
      @Override
      public void onMessage(final String message) {
        messageReceived.complete(message);
      }
    });
    final String message = "Hello world!";

    final ClientDescriptor fakeClient = mock(ClientDescriptor.class);
    proxyInvoker.addClient(fakeClient);
    proxyInvoker.fireMessage(String.class, message, false);

    assertThat(messageReceived.get(5, TimeUnit.SECONDS), equalTo(message));
  }

  @Test
  public void testClientInvokeInitiatedMessageFiring() throws ExecutionException, InterruptedException {
    final SerializationCodec codec = new SerializationCodec();
    final MessageListener<Integer> listener = new MessageListener<Integer>() {
      @Override
      public void onMessage(final Integer message) {
      }
    };
    final FiringClientIdAware firingClientIdAware = new FiringClientIdAware();
    final MessageCodec<ProxyEntityMessage, ProxyEntityResponse> msgCodec = new ProxyMessageCodec(ClientIdAware.class, new Class[] {Integer.class});
    final ProxyInvoker<ClientIdAware> proxyInvoker = new ProxyInvoker<ClientIdAware>(firingClientIdAware).activateEvents(new ClientCommunicator() {
      @Override
      public void closeClientConnection(ClientDescriptor clientDescriptor) {
        
      }

      public void sendNoResponse(final ClientDescriptor clientDescriptor, final EntityResponse message) {
        ProxyEntityResponse pem = (ProxyEntityResponse) message;
        listener.onMessage((Integer) pem.getResponse());
      }

      public Future<Void> send(final ClientDescriptor clientDescriptor, final EntityResponse message) {
        throw new UnsupportedOperationException("Implement me!");
      }
    }, new Class[] {Integer.class});
    final EntityClientEndpoint endpoint = mock(EntityClientEndpoint.class);
    final MyClientDescriptor myClient = new MyClientDescriptor();
    when(endpoint.message(any())).thenAnswer(invocation -> new RecordingInvocation(proxyInvoker, msgCodec, invocation.getArgument(0), myClient));
    proxyInvoker.addClient(new MyClientDescriptor());
    proxyInvoker.addClient(myClient);

    final ClientIdAware proxy = ClientProxyFactory.createProxy(ClientIdAware.class, ClientIdAware.class, endpoint, new Class[] {Integer.class}, codec);
    proxy.registerMessageListener(Integer.class, listener);
    proxy.nothing();
    proxy.notMuch(null);
    assertThat(firingClientIdAware.counter.get(), is(1));
  }

  @Test
  public void testClientIdSubstitution() throws ExecutionException, InterruptedException {
    final SerializationCodec codec = new SerializationCodec();
    final MessageCodec<ProxyEntityMessage, ProxyEntityResponse> messageCodec = new ProxyMessageCodec(ClientIdAware.class, null);
    final ProxyInvoker<ClientIdAware> proxyInvoker = new ProxyInvoker<ClientIdAware>(new ClientIdAware() {
      public <T> void registerMessageListener(Class<T> type, final MessageListener<T> listener) {
        throw new UnsupportedOperationException("Implement me!");
      }

      public void nothing() {
        //
      }

      public void notMuch(final Object id) {
        assertThat(id, notNullValue());
        assertThat(id, instanceOf(MyClientDescriptor.class));
      }

      public Serializable much(final Serializable foo, final Object id) {
        notMuch(id);
        return "YAY!";
      }
    });
    final EntityClientEndpoint endpoint = mock(EntityClientEndpoint.class);
    when(endpoint.message(any())).thenAnswer(invocation -> new RecordingInvocation(proxyInvoker, messageCodec, invocation.getArgument(0)));


    final ClientIdAware proxy = ClientProxyFactory.createProxy(ClientIdAware.class, ClientIdAware.class, endpoint, null, codec);
    proxy.nothing();
    proxy.notMuch(null);
    assertThat(proxy.much(12, 12), notNullValue());
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static class RecordingInvocation implements Invocation<ProxyEntityResponse> {
    private final ProxyInvoker<?> proxyInvoker;
    private final ProxyEntityMessage message;
    private final MyClientDescriptor clientDescriptor;

    public RecordingInvocation(final ProxyInvoker<?> proxyInvoker, MessageCodec<ProxyEntityMessage, ProxyEntityResponse> codec, ProxyEntityMessage message) {
      this(proxyInvoker, codec, message, new MyClientDescriptor());
    }

    public RecordingInvocation(final ProxyInvoker<?> proxyInvoker, MessageCodec<ProxyEntityMessage, ProxyEntityResponse> codec, ProxyEntityMessage message, final MyClientDescriptor clientDescriptor) {
      this.proxyInvoker = proxyInvoker;
      this.clientDescriptor = clientDescriptor;
      this.message = message;
    }

    @Override
    public Task invoke(InvocationCallback<ProxyEntityResponse> callback, Set<InvocationCallback.Types> callbacks) {
      callback.sent();
      callback.received();
      try {
        callback.result(proxyInvoker.invoke(new ActiveInvokeContext() {
          @Override
          public ClientDescriptor getClientDescriptor() {
            return clientDescriptor;
          }

          @Override
          public ActiveInvokeChannel openInvokeChannel() {
            throw new UnsupportedOperationException("Not supported yet.");
          }

          @Override
          public ClientSourceId getClientSource() {
            return null;
          }

          @Override
          public long getCurrentTransactionId() {
            return 0;
          }

          @Override
          public long getOldestTransactionId() {
            return 0;
          }

          @Override
          public boolean isValidClientInformation() {
            return false;
          }

          @Override
          public ClientSourceId makeClientSourceId(long l) {
            return null;
          }

          @Override
          public int getConcurrencyKey() {
            return 0;
          }

          @Override
          public Properties getClientSourceProperties() {
            return new Properties();
          }
        }, message));
      } catch (Throwable t) {
        callback.failure(t);
      } finally {
        callback.complete();
        callback.retired();
      }
      return () -> false;
    }

  }

  private static class MyClientDescriptor implements ClientDescriptor {
    @Override
    public ClientSourceId getSourceId() {
      return null;
    }

    @Override
    public boolean isValidClient() {
      return false;
    }
  }

  public interface ClientIdAware extends ServerMessageAware {

    void nothing();

    void notMuch(@ClientId Object id);

    Serializable much(Serializable foo, @ClientId Object id);

  }

  public interface ComparableEntity extends ServerMessageAware, Entity, Comparable {

  }

  private static class FiringClientIdAware implements ClientIdAware, MessageFiringSupport {

    private final AtomicInteger counter = new AtomicInteger();
    private MessageFiring messageFiring;

    @Override
    public void setMessageFiring(MessageFiring messageFiring) {
      this.messageFiring = messageFiring;
    }

    public void nothing() {
      //
    }

    public void notMuch(final Object id) {
      messageFiring.fireMessage(Integer.class, counter.getAndIncrement(), false);
    }

    public Serializable much(final Serializable foo, final Object id) {
      notMuch(id);
      return "YAY!";
    }

    @Override
    public <T> void registerMessageListener(Class<T> type, final MessageListener<T> listener) {
      // noop
    }
  }
}
