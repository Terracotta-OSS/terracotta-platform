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

package org.terracotta.voltron.proxy.server;

import org.terracotta.entity.ClientDescriptor;
import org.terracotta.voltron.proxy.ClientId;
import org.terracotta.voltron.proxy.Codec;
import org.terracotta.voltron.proxy.CommonProxyFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;

/**
 * @author Alex Snaps
 */
public class ProxyInvoker<T> {

  private final T target;
  private final Codec codec;
  private final Map<Byte, Method> mappings;

  public ProxyInvoker(Class<T> proxyType, T target, Codec codec) {
    this.target = target;
    this.codec = codec;
    this.mappings = createMethodMappings(proxyType);
  }

  public byte[] invoke(final ClientDescriptor clientDescriptor, final byte[] arg) {
    final Method method = mappings.get(arg[0]);
    if(method == null) {
      throw new AssertionError();
    }
    final Object[] args = codec.decode(Arrays.copyOfRange(arg, 1, arg.length), method.getParameterTypes());
    try {
      final Annotation[][] allAnnotations = method.getParameterAnnotations();
      for (int i = 0; i < allAnnotations.length; i++) {
        for (Annotation parameterAnnotation : allAnnotations[i]) {
          if (parameterAnnotation.annotationType() == ClientId.class) {
            args[i] = clientDescriptor;
            break;
          }
        }
      }
      return codec.encode(method.getReturnType(), method.invoke(target, args));
    } catch (IllegalAccessException e) {
      throw new IllegalArgumentException(e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e.getCause());
    }
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

}
