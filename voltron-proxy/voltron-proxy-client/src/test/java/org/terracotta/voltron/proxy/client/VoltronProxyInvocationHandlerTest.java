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
import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.InvocationBuilder;
import org.terracotta.entity.InvokeFuture;
import org.terracotta.voltron.proxy.ClientId;
import org.terracotta.voltron.proxy.MethodDescriptor;
import org.terracotta.voltron.proxy.ProxyEntityMessage;
import org.terracotta.voltron.proxy.ProxyEntityResponse;
import org.terracotta.voltron.proxy.SerializationCodec;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.terracotta.voltron.proxy.CommonProxyFactory.createMethodMappings;
import static org.terracotta.voltron.proxy.CommonProxyFactory.invert;

/**
 * @author Alex Snaps
 */
public class VoltronProxyInvocationHandlerTest {

  @Test
  public void testNullsClientIdAnnotatedParams() throws Throwable {


    final Set<Object> valuesSeen = new HashSet<Object>();
    final SerializationCodec codec = new SerializationCodec() {
      @Override
      public byte[] encode(final Class<?> type, final Object value) {
        valuesSeen.add(value);
        return super.encode(type, value);
      }
    };
    final EntityClientEndpoint<ProxyEntityMessage, ProxyEntityResponse> endpoint = mock(EntityClientEndpoint.class);
    final InvocationBuilder builder = mock(InvocationBuilder.class);
    when(endpoint.beginInvoke()).thenReturn(builder);
    when(builder.message(Matchers.<EntityMessage>any())).thenReturn(builder);
    final InvokeFuture future = mock(InvokeFuture.class);
    when(builder.invoke()).thenReturn(future);
    when(future.get()).thenReturn(ProxyEntityResponse.messageResponse(Void.TYPE, null));

    Map<MethodDescriptor, Byte> methodMappings = invert(createMethodMappings(TestInterface.class));
    VoltronProxyInvocationHandler handler = new VoltronProxyInvocationHandler(endpoint, Collections.<Class<?>>emptyList(), codec);
    for (MethodDescriptor method : methodMappings.keySet()) {
      handler.invoke(null, method.getMethod(), new Object[] { "String", new Object() });
    }
    for (Object objects : valuesSeen) {
      assertNull(((Object[]) objects)[1]);
    }
  }

  interface TestInterface {

    void testing(Object foo, @ClientId Object bar);

  }

}