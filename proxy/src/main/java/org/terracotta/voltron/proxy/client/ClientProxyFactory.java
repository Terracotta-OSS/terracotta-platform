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
import org.terracotta.voltron.proxy.Codec;
import org.terracotta.voltron.proxy.SerializationCodec;
import org.terracotta.voltron.proxy.client.messages.ServerMessageAware;

import java.lang.reflect.Proxy;

import static org.terracotta.voltron.proxy.CommonProxyFactory.createMethodMappings;
import static org.terracotta.voltron.proxy.CommonProxyFactory.createResponseTypeMappings;
import static org.terracotta.voltron.proxy.CommonProxyFactory.invert;

/**
 * @author Alex Snaps
 */
public class ClientProxyFactory {

  public static <T, R extends Entity & ServerMessageAware> R createEntityProxy(Class<T> clientType, Class<? super T> type,
                                                                               EntityClientEndpoint entityClientEndpoint,
                                                                               Class<?> messageType, Class<?>... messageTypes) {
    return (R) createProxy(clientType, type, entityClientEndpoint, new SerializationCodec(), sum(messageType, messageTypes));
  }

  public static <T, R extends Entity & ServerMessageAware> R createEntityProxy(Class<T> clientType, Class<? super T> type,
                                                                               EntityClientEndpoint entityClientEndpoint,
                                                                               final Codec codec, Class<?> messageType,
                                                                               Class<?>... messageTypes) {
    return (R) createProxy(clientType, type, entityClientEndpoint, codec, sum(messageType, messageTypes));
  }

  public static <T, R extends Entity> R createEntityProxy(Class<T> clientType, Class<? super T> type,
                                                       EntityClientEndpoint entityClientEndpoint) {
    return (R) createProxy(clientType, type, entityClientEndpoint);
  }

  public static <T, R extends Entity> R createEntityProxy(Class<T> clientType, Class<? super T> type,
                                                       EntityClientEndpoint entityClientEndpoint, final Codec codec) {
    return (R) createProxy(clientType, type, entityClientEndpoint, codec);
  }

  public static <T> T createProxy(Class<T> clientType, Class<? super T> type, EntityClientEndpoint entityClientEndpoint) {
    return createProxy(clientType, type, entityClientEndpoint, new SerializationCodec());
  }

  public static <T> T createProxy(Class<T> clientType, Class<? super T> type, EntityClientEndpoint entityClientEndpoint,
                                  final Codec codec, Class... messageTypes) {
    
    if (entityClientEndpoint == null) {
      throw new NullPointerException("EntityClientEndpoint has to be provided!");
    }

    if (!type.isInterface()) {
      throw new IllegalArgumentException("We only proxy interfaces!");
    }

    final Class[] interfaces;
    if (messageTypes.length == 0) {
      interfaces = new Class[] { clientType, Entity.class };
    } else {
      interfaces = new Class[] { clientType, Entity.class, ServerMessageAware.class };
    }
    return clientType.cast(Proxy.newProxyInstance(Entity.class.getClassLoader(), interfaces,
        new VoltronProxyInvocationHandler(
                invert(createMethodMappings(type)),
                entityClientEndpoint, codec,
                invert(createResponseTypeMappings(type, messageTypes))
        )
    ));
  }

  private static Class<?>[] sum(Class<?> one, Class<?>[] others) {
    Class<?>[] result = new Class<?>[others.length + 1];
    result[0] = one;
    System.arraycopy(others, 0, result, 1, others.length);
    return result;
  }

}
