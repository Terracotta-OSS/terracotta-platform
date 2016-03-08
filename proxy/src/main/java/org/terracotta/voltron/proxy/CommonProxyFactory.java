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
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableMap;

/**
 * @author Alex Snaps
 */
public class CommonProxyFactory {
  private static final Comparator<MethodDescriptor> METHOD_COMPARATOR = new Comparator<MethodDescriptor>() {
    public int compare(final MethodDescriptor m1, final MethodDescriptor m2) {
      return m1.toGenericString().compareTo(m2.toGenericString());
    }
  };

  private static final Comparator<Class<?>> CLASS_COMPARATOR = new Comparator<Class<?>>() {
    public int compare(final Class<?> o1, final Class<?> o2) {
      return o1.getName().compareTo(o2.getName());
    }
  };

  public static <T, U> Map<U, T> invert(Map<T, U> map) {
    Map<U, T> inversion = new HashMap<U, T>();
    for (Entry<T, U> e : map.entrySet()) {
      if (inversion.put(e.getValue(), e.getKey()) != null) {
        throw new IllegalArgumentException("Inversion is not a valid map");
      }
    }
    return unmodifiableMap(inversion);
  }

  public static Map<Byte, MethodDescriptor> createMethodMappings(final Class<?> proxyType) {
    SortedSet<MethodDescriptor> methods = getSortedMethods(proxyType);

    final HashMap<Byte, MethodDescriptor> map = new HashMap<Byte, MethodDescriptor>();
    byte index = 0;
    for (final MethodDescriptor method : methods) {
      map.put(index++, method);
    }
    return map;
  }

  public static Map<Class<?>, Byte> createResponseTypeMappings(Class<?> proxyType, Class<?> ... events) {
    final HashMap<Class<?>, Byte> map = new HashMap<Class<?>, Byte>();
    byte index = 0;
    for (MethodDescriptor m : getSortedMethods(proxyType)) {
      Class<?> responseType = m.getMessageType();
      if (!map.containsKey(responseType)) {
        map.put(responseType, index++);
      }
    }
    for (Class<?> eventType : getSortedTypes(events)) {
      if (!map.containsKey(eventType)) {
        map.put(eventType, index++);
      }
    }
    return unmodifiableMap(map);
  }

  static SortedSet<MethodDescriptor> getSortedMethods(final Class<?> type) {
    SortedSet<MethodDescriptor> methods = new TreeSet<MethodDescriptor>(METHOD_COMPARATOR);

    final Method[] declaredMethods = type.getDeclaredMethods();

    if (declaredMethods.length > 256) {
      throw new IllegalArgumentException("Can't proxy that many methods on a single instance!");
    }

    for (Method declaredMethod : declaredMethods) {
      methods.add(MethodDescriptor.of(declaredMethod));
    }

    if (methods.size() != declaredMethods.length) {
      throw new AssertionError("Ouch... looks like that didn't work!");
    }
    return methods;
  }

  private static SortedSet<Class<?>> getSortedTypes(Class<?> ... types) {
    final TreeSet<Class<?>> classes = new TreeSet<Class<?>>(CLASS_COMPARATOR);
    classes.addAll(asList(types));
    return classes;
  }
}
