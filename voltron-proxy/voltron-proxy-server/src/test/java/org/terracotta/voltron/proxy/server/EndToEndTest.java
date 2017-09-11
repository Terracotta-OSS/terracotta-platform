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

import org.junit.Test;
import org.terracotta.connection.entity.Entity;
import org.terracotta.entity.ActiveInvokeContext;
import org.terracotta.entity.ClientCommunicator;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.ClientSourceId;
import org.terracotta.entity.EndpointDelegate;
import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.InvocationBuilder;
import org.terracotta.entity.InvokeContext;
import org.terracotta.entity.InvokeFuture;
import org.terracotta.entity.MessageCodec;
import org.terracotta.exception.EntityException;
import org.terracotta.voltron.proxy.ClientId;
import org.terracotta.voltron.proxy.MessageListener;
import org.terracotta.voltron.proxy.ProxyEntityMessage;
import org.terracotta.voltron.proxy.ProxyEntityResponse;
import org.terracotta.voltron.proxy.ProxyMessageCodec;
import org.terracotta.voltron.proxy.SerializationCodec;
import org.terracotta.voltron.proxy.client.ClientProxyFactory;
import org.terracotta.voltron.proxy.client.ServerMessageAware;

import java.io.Serializable;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Alex Snaps
 */
public class EndToEndTest {

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
    final RecordingInvocationBuilder builder = new RecordingInvocationBuilder(proxyInvoker, messageCodec);
    when(endpoint.beginInvoke()).thenReturn(builder);


    final Comparable proxy = ClientProxyFactory.createProxy(Comparable.class, Comparable.class, endpoint, null, codec);
    assertThat(proxy.compareTo("blah!"), is(42));
  }

  @Test
  public void testServerInitiatedMessageFiring() throws ExecutionException, InterruptedException {
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
    final RecordingInvocationBuilder builder = new RecordingInvocationBuilder(proxyInvoker, messageCodec);
    final EntityClientEndpoint endpoint = new EntityClientEndpoint() {
      public byte[] getEntityConfiguration() {
        throw new UnsupportedOperationException("Implement me!");
      }

      public void setDelegate(final EndpointDelegate endpointDelegate) {
        delegate.set(endpointDelegate);
      }

      public InvocationBuilder beginInvoke() {
        return builder;
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
    final AtomicReference<String> messageReceived = new AtomicReference<String>();
    proxy.registerMessageListener(String.class, new MessageListener<String>() {
      @Override
      public void onMessage(final String message) {
        messageReceived.set(message);
      }
    });
    final String message = "Hello world!";

    final ClientDescriptor fakeClient = mock(ClientDescriptor.class);
    proxyInvoker.addClient(fakeClient);
    proxyInvoker.fireMessage(String.class, message, false);

    assertThat(messageReceived.get(), equalTo(message));
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
    final RecordingInvocationBuilder builder = new RecordingInvocationBuilder(proxyInvoker, msgCodec, myClient);
    when(endpoint.beginInvoke()).thenReturn(builder);
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
    final RecordingInvocationBuilder builder = new RecordingInvocationBuilder(proxyInvoker, messageCodec);
    when(endpoint.beginInvoke()).thenReturn(builder);


    final ClientIdAware proxy = ClientProxyFactory.createProxy(ClientIdAware.class, ClientIdAware.class, endpoint, null, codec);
    proxy.nothing();
    proxy.notMuch(null);
    assertThat(proxy.much(12, 12), notNullValue());
  }

  private static class RecordingInvocationBuilder implements InvocationBuilder<ProxyEntityMessage, ProxyEntityResponse> {
    private final MessageCodec<ProxyEntityMessage, ProxyEntityResponse> codec;
    private final ProxyInvoker<?> proxyInvoker;
    private ProxyEntityMessage message;
    private MyClientDescriptor clientDescriptor;

    public RecordingInvocationBuilder(final ProxyInvoker<?> proxyInvoker, MessageCodec<ProxyEntityMessage, ProxyEntityResponse> codec) {
      this(proxyInvoker, codec, new MyClientDescriptor());
    }

    public RecordingInvocationBuilder(final ProxyInvoker<?> proxyInvoker, MessageCodec<ProxyEntityMessage, ProxyEntityResponse> codec, final MyClientDescriptor clientDescriptor) {
      this.proxyInvoker = proxyInvoker;
      this.clientDescriptor = clientDescriptor;
      this.codec = codec;
    }

    @Override
    public InvocationBuilder<ProxyEntityMessage, ProxyEntityResponse> ackReceived() {
      return this;
    }

    @Override
    public InvocationBuilder<ProxyEntityMessage, ProxyEntityResponse> ackCompleted() {
      return this;
    }

    @Override
    public InvocationBuilder<ProxyEntityMessage, ProxyEntityResponse> ackRetired() {
      return this;
    }

    @Override
    public InvocationBuilder<ProxyEntityMessage, ProxyEntityResponse> ackSent() {
      return this;
    }

    @Override
    public InvocationBuilder<ProxyEntityMessage, ProxyEntityResponse> replicate(final boolean b) {
      return this;
    }

    @Override
    public InvocationBuilder<ProxyEntityMessage, ProxyEntityResponse> message(ProxyEntityMessage bytes) {
      message = bytes;
      return this;
    }

    @Override
    public InvocationBuilder<ProxyEntityMessage, ProxyEntityResponse> blockGetOnRetire(boolean shouldBlock) {
      return this;
    }

    @Override
    public InvokeFuture<ProxyEntityResponse> invoke() {
      final FutureTask<ProxyEntityResponse> futureTask = new FutureTask<ProxyEntityResponse>(new Callable<ProxyEntityResponse>() {
        @Override
        public ProxyEntityResponse call() throws Exception {
          return proxyInvoker.invoke(new ActiveInvokeContext() {
            @Override
            public ClientDescriptor getClientDescriptor() {
              return clientDescriptor;
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
          }, message);
        }
      });
      futureTask.run();
      return new InvokeFuture<ProxyEntityResponse>() {
        @Override
        public boolean isDone() {
          throw new UnsupportedOperationException("Implement me!");
        }

        @Override
        public ProxyEntityResponse get() throws InterruptedException, EntityException {
          try {
            return futureTask.get();
          } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
          }
        }

        @Override
        public ProxyEntityResponse getWithTimeout(final long l, final TimeUnit timeUnit) throws InterruptedException, EntityException, TimeoutException {
          throw new UnsupportedOperationException("Implement me!");
        }

        @Override
        public void interrupt() {
          throw new UnsupportedOperationException("Implement me!");
        }
      };
    }

  }

  private static class MyClientDescriptor implements ClientDescriptor {
    @Override
    public ClientSourceId getSourceId() {
      return null;
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
