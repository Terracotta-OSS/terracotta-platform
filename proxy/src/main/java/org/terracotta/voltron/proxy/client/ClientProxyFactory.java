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
import org.terracotta.voltron.proxy.CommonProxyFactory;
import org.terracotta.voltron.proxy.SerializationCodec;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;

/**
 * @author Alex Snaps
 */
public class ClientProxyFactory {

  public static <T, R extends Entity> R createEntityProxy(Class<T> type,
                                                       EntityClientEndpoint entityClientEndpoint) {
    return (R) createProxy(type, entityClientEndpoint);
  }

  public static <T, R extends Entity> R createEntityProxy(Class<T> type,
                                                       EntityClientEndpoint entityClientEndpoint, final Codec codec) {
    return (R) createProxy(type, entityClientEndpoint, codec);
  }

  public static <T> T createProxy(Class<T> type, EntityClientEndpoint entityClientEndpoint) {
    return createProxy(type, entityClientEndpoint, new SerializationCodec());
  }

  public static <T> T createProxy(Class<T> type, EntityClientEndpoint entityClientEndpoint, final Codec codec) {

    if (entityClientEndpoint == null) {
      throw new NullPointerException("EntityClientEndpoint has to be provided!");
    }

    if (!type.isInterface()) {
      throw new IllegalArgumentException("We only proxy interfaces!");
    }

    Map<Method, Byte> mappings = createMethodMappings(type);

    return type.cast(Proxy.newProxyInstance(Entity.class.getClassLoader(), new Class[] { type, Entity.class },
        new VoltronProxyInvocationHandler(mappings, entityClientEndpoint, codec)));
  }

  static Map<Method, Byte> createMethodMappings(final Class type) {
    SortedSet<Method> methods = CommonProxyFactory.getSortedMethods(type);

    final HashMap<Method, Byte> map = new HashMap<Method, Byte>();
    byte index = 0;
    for (final Method method : methods) {
      map.put(method, index++);
    }
    return map;
  }

}
