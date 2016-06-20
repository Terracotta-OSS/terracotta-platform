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

package org.terracotta.voltron.proxy;

import org.junit.Test;
import org.terracotta.connection.entity.Entity;
import org.terracotta.entity.ClientCommunicator;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.EndpointDelegate;
import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.InvocationBuilder;
import org.terracotta.entity.InvokeFuture;
import org.terracotta.entity.MessageCodec;
import org.terracotta.exception.EntityException;
import org.terracotta.voltron.proxy.client.ClientProxyFactory;
import org.terracotta.voltron.proxy.client.messages.MessageListener;
import org.terracotta.voltron.proxy.client.messages.ServerMessageAware;
import org.terracotta.voltron.proxy.server.ProxyInvoker;
import org.terracotta.voltron.proxy.server.messages.MessageFiring;
import org.terracotta.voltron.proxy.server.messages.ProxyEntityMessage;
import org.terracotta.voltron.proxy.server.messages.ProxyEntityResponse;

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
    final Codec codec = new SerializationCodec();
    final ProxyMessageCodec messageCodec = new ProxyMessageCodec(codec, Comparable.class);
    final ProxyInvoker<Comparable> proxyInvoker = new ProxyInvoker<Comparable>(new Comparable() {
      public int compareTo(final Object o) {
        return 42;
      }
    });
    final EntityClientEndpoint endpoint = mock(EntityClientEndpoint.class);
    final RecordingInvocationBuilder builder = new RecordingInvocationBuilder(proxyInvoker, messageCodec);
    when(endpoint.beginInvoke()).thenReturn(builder);


    final Comparable proxy = ClientProxyFactory.createProxy(Comparable.class, Comparable.class, endpoint, codec);
    assertThat(proxy.compareTo("blah!"), is(42));
  }

  @Test
  public void testServerInitiatedMessageFiring() throws ExecutionException, InterruptedException {
    final AtomicReference<EndpointDelegate> delegate = new AtomicReference<EndpointDelegate>();
    final Codec codec = new SerializationCodec();
    final ProxyMessageCodec messageCodec = new ProxyMessageCodec(codec, Comparable.class, String.class);
    final ProxyInvoker<Comparable> proxyInvoker = new ProxyInvoker<Comparable>(new Comparable() {
      public int compareTo(final Object o) {
        return 42;
      }
    }, new ClientCommunicator() {
      public void sendNoResponse(final ClientDescriptor clientDescriptor, final EntityResponse message) {
        throw new UnsupportedOperationException("Implement me!");
      }

      public Future<Void> send(final ClientDescriptor clientDescriptor, final EntityResponse message) {

        final FutureTask<Void> voidFutureTask = new FutureTask<Void>(new Callable<Void>() {
          public Void call() throws Exception {
            return null;
          }
        });
        voidFutureTask.run();
        delegate.get().handleMessage(message);
        return voidFutureTask;
      }
    }, String.class);
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

      public void didCloseUnexpectedly() {
        throw new UnsupportedOperationException("Implement me!");
      }
    };

    final ComparableEntity proxy = ClientProxyFactory.createEntityProxy(ComparableEntity.class, Comparable.class, endpoint, codec, String.class);
    final AtomicReference<String> messageReceived = new AtomicReference<String>();
    proxy.registerListener(new MessageListener<String>() {
      @Override
      public void onMessage(final String message) {
        messageReceived.set(message);
      }
    });
    final String message = "Hello world!";

    final ClientDescriptor fakeClient = mock(ClientDescriptor.class);
    proxyInvoker.addClient(fakeClient);
    proxyInvoker.fireMessage(message);

    assertThat(messageReceived.get(), equalTo(message));
  }

  @Test
  public void testClientInvokeInitiatedMessageFiring() throws ExecutionException, InterruptedException {
    final Codec codec = new SerializationCodec();
    final MessageListener<Integer> listener = new MessageListener<Integer>() {
      @Override
      public void onMessage(final Integer message) {
      }
    };
    final FiringClientIdAware firingClientIdAware = new FiringClientIdAware();
    final MessageCodec<ProxyEntityMessage, ProxyEntityResponse> msgCodec = new ProxyMessageCodec(codec, ClientIdAware.class, Integer.class);
    final ProxyInvoker<ClientIdAware> proxyInvoker = new ProxyInvoker<ClientIdAware>(firingClientIdAware, new ClientCommunicator() {
      public void sendNoResponse(final ClientDescriptor clientDescriptor, final EntityResponse message) {
        throw new UnsupportedOperationException("Implement me!");
      }

      public Future<Void> send(final ClientDescriptor clientDescriptor, final EntityResponse message) {
        final FutureTask<Void> voidFutureTask = new FutureTask<Void>(new Callable<Void>() {
          public Void call() throws Exception {
            ProxyEntityResponse pem = (ProxyEntityResponse) message;
            listener.onMessage((Integer) pem.getResponse());
            return null;
          }
        });
        voidFutureTask.run();
        return voidFutureTask;
      }
    }, Integer.class);
    final EntityClientEndpoint endpoint = mock(EntityClientEndpoint.class);
    final MyClientDescriptor myClient = new MyClientDescriptor();
    final RecordingInvocationBuilder builder = new RecordingInvocationBuilder(proxyInvoker, msgCodec, myClient);
    when(endpoint.beginInvoke()).thenReturn(builder);
    proxyInvoker.addClient(new MyClientDescriptor());
    proxyInvoker.addClient(myClient);

    final ClientIdAware proxy = ClientProxyFactory.createProxy(ClientIdAware.class, ClientIdAware.class, endpoint, codec, Integer.class);
    proxy.registerListener(listener);
    proxy.nothing();
    proxy.notMuch(null);
    assertThat(firingClientIdAware.counter.get(), is(1));
  }

  @Test
  public void testClientIdSubstitution() throws ExecutionException, InterruptedException {
    final Codec codec = new SerializationCodec();
    final MessageCodec<ProxyEntityMessage, ProxyEntityResponse> messageCodec = new ProxyMessageCodec(codec, ClientIdAware.class);
    final ProxyInvoker<ClientIdAware> proxyInvoker = new ProxyInvoker<ClientIdAware>(new ClientIdAware() {
      public void registerListener(final MessageListener<Integer> listener) {
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


    final ClientIdAware proxy = ClientProxyFactory.createProxy(ClientIdAware.class, ClientIdAware.class, endpoint, codec);
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
          return proxyInvoker.invoke(clientDescriptor, message);
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

  private static class MyClientDescriptor implements ClientDescriptor {}

  public interface ClientIdAware extends ServerMessageAware<Integer> {

    void nothing();

    void notMuch(@ClientId Object id);

    Serializable much(Serializable foo, @ClientId Object id);

  }

  public interface ComparableEntity extends ServerMessageAware, Entity, Comparable {

  }

  private static class FiringClientIdAware extends MessageFiring implements ClientIdAware {

    private final AtomicInteger counter = new AtomicInteger();

    public FiringClientIdAware() {
      super(Integer.class);
    }

    public void nothing() {
      //
    }

    public void notMuch(final Object id) {
      fire(counter.getAndIncrement());
    }

    public Serializable much(final Serializable foo, final Object id) {
      notMuch(id);
      return "YAY!";
    }

    public void registerListener(final MessageListener<Integer> listener) {
      // noop
    }
  }
}
