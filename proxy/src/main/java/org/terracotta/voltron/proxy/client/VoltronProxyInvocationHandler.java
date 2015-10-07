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

import org.terracotta.connection.entity.Entity;
import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.entity.InvocationBuilder;
import org.terracotta.voltron.proxy.Async;
import org.terracotta.voltron.proxy.Codec;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Alex Snaps
 */
class VoltronProxyInvocationHandler implements InvocationHandler {

  private static final Method close;

  static {
    try {
      close = Entity.class.getDeclaredMethod("close");
    } catch (NoSuchMethodException e) {
      throw new AssertionError("Someone changed some method signature here!!!");
    }
  }

  private final Map<Method, Byte> mappings;
  private final EntityClientEndpoint entityClientEndpoint;
  private final Codec codec;

  public VoltronProxyInvocationHandler(final Map<Method, Byte> mappings,
                                       final EntityClientEndpoint entityClientEndpoint,
                                       final Codec codec) {
    this.mappings = mappings;
    this.entityClientEndpoint = entityClientEndpoint;
    this.codec = codec;
  }

  public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {

    if(close.equals(method)) {
      entityClientEndpoint.close();
      return null;
    }

    final byte[] payload = encode(method, args);

    final InvocationBuilder builder = entityClientEndpoint.beginInvoke()
        .payload(payload);

    final Type returnType = method.getGenericReturnType();
    if (method.getReturnType() == Future.class && returnType instanceof ParameterizedType) {
      final Type decodeTo = ((ParameterizedType)returnType).getActualTypeArguments()[0];

      final Async annotation = method.getAnnotation(Async.class);
      if (annotation != null) {
        switch (annotation.value()) {
          case RECEIVED:
            builder.ackReceived();
            break;
          case NONE:
            break;
          default:
            throw new IllegalArgumentException("Unknown Async annotation value of :" + annotation.value());
        }
      }

      final Future<byte[]> future = builder
          .invoke();
      return new Future() {
        public boolean cancel(final boolean mayInterruptIfRunning) {
          return future.cancel(mayInterruptIfRunning);
        }

        public boolean isCancelled() {
          return future.isCancelled();
        }

        public boolean isDone() {
          return future.isDone();
        }

        public Object get() throws InterruptedException, ExecutionException {
          return codec.decode(future.get(), (Class<?>) decodeTo);
        }

        public Object get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
          return codec.decode(future.get(timeout, unit), (Class<?>) decodeTo);
        }
      };
    } else {
      return codec.decode(builder.invoke().get(), method.getReturnType());
    }
  }

  private byte[] encode(final Method method, final Object[] args) throws IOException {

    final Byte methodIdentifier = mappings.get(method);

    if(methodIdentifier == null) {
      throw new AssertionError("WAT, no mapping for " + method.toGenericString());
    }

    ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
    DataOutputStream output = new DataOutputStream(byteOut);

    final Class<?>[] parameterTypes = method.getParameterTypes();
    output.writeByte(methodIdentifier);
    output.write(codec.encode(parameterTypes, args));

    output.close();
    return byteOut.toByteArray();
  }

}
