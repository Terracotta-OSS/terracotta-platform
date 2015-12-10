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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import org.terracotta.entity.MessageCodec;
import org.terracotta.voltron.proxy.server.messages.ProxyEntityMessage;
import org.terracotta.voltron.proxy.server.messages.ProxyEntityResponse;

/**
 *
 * @author cdennis
 */
public class ProxyMessageCodec implements MessageCodec<ProxyEntityMessage, ProxyEntityResponse> {

  private final Codec codec;
  private final Map<Byte, Method> mappings;

  public ProxyMessageCodec(Codec codec, Class<?> proxyType, Class<?> ... messageTypes) {
    this.codec = codec;
    this.mappings = createMethodMappings(proxyType);
  }

  static Map<Byte, Method> createMethodMappings(final Class type) {
    SortedSet<Method> methods = CommonProxyFactory.getSortedMethods(type);

    final HashMap<Byte, Method> map = new HashMap<Byte, Method>();
    byte index = 0;
    for (final Method method : methods) {
      map.put(index++, method);
    }
    return map;
  }

  public byte[] serialize(ProxyEntityResponse r) {
    return codec.encode(r.getResponseType(), r.getResponse());
  }

  public ProxyEntityMessage deserialize(final byte[] bytes) {
    final Method method = decodeMethod(bytes[0]);
    return new ProxyEntityMessage(method, decodeArgs(bytes, method.getParameterTypes()));
  }

  public ProxyEntityMessage deserializeForSync(final int i, final byte[] bytes) {
    throw new UnsupportedOperationException("Implement me!");
  }

  private Method decodeMethod(final byte b) {
    final Method method = mappings.get(b);
    if(method == null) {
      throw new AssertionError();
    }
    return method;
  }

  private Object[] decodeArgs(final byte[] arg, final Class<?>[] parameterTypes) {
    return codec.decode(Arrays.copyOfRange(arg, 1, arg.length), parameterTypes);
  }
}
