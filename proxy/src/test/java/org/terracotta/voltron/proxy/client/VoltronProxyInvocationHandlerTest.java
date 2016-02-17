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
import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.entity.InvocationBuilder;
import org.terracotta.entity.InvokeFuture;
import org.terracotta.voltron.proxy.ClientId;
import org.terracotta.voltron.proxy.ProxyMessageCodec;
import org.terracotta.voltron.proxy.SerializationCodec;
import org.terracotta.voltron.proxy.server.messages.ProxyEntityResponse;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;
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


    final Set<Object[]> valuesSeen = new HashSet<Object[]>();
    final SerializationCodec codec = new SerializationCodec() {
      @Override
      public byte[] encode(final Class<?>[] type, final Object[] values) {
        valuesSeen.add(values);
        return super.encode(type, values);
      }
    };
    final ProxyMessageCodec messageCodec = new ProxyMessageCodec(codec, TestInterface.class);
    final EntityClientEndpoint endpoint = mock(EntityClientEndpoint.class);
    final InvocationBuilder builder = mock(InvocationBuilder.class);
    when(endpoint.beginInvoke()).thenReturn(builder);
    when(builder.payload(Matchers.<byte[]>any())).thenReturn(builder);
    final InvokeFuture future = mock(InvokeFuture.class);
    when(builder.invoke()).thenReturn(future);
    when(future.get()).thenReturn(messageCodec.serialize(ProxyEntityResponse.response(Void.TYPE, null)));

    Map<Method, Byte> methodMappings = invert(createMethodMappings(TestInterface.class));
    VoltronProxyInvocationHandler handler = new VoltronProxyInvocationHandler(methodMappings, endpoint, codec, Collections.<Byte, Class<?>>emptyMap());
    for (Method method : methodMappings.keySet()) {
      handler.invoke(null, method, new Object[] { "String", new Object() });
    }
    for (Object[] objects : valuesSeen) {
      assertNull(objects[1]);
    }
  }

  interface TestInterface {

    void testing(Object foo, @ClientId Object bar);

  }

}