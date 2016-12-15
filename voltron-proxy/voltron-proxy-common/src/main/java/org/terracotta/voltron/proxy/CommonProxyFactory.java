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

  public static Map<Class<?>, Byte> createResponseTypeMappings(Class<?> proxyType) {
    return createResponseTypeMappings(proxyType, null);
  }

  public static Map<Class<?>, Byte> createResponseTypeMappings(Class<?> proxyType, Class<?>[] events) {
    final HashMap<Class<?>, Byte> map = new HashMap<Class<?>, Byte>();
    byte index = 0;
    for (MethodDescriptor m : getSortedMethods(proxyType)) {
      Class<?> responseType = m.getMessageType();
      if (!map.containsKey(responseType)) {
        map.put(responseType, index++);
      }
    }
    if (events != null) {
      for (Class<?> eventType : getSortedTypes(events)) {
        if (!map.containsKey(eventType)) {
          map.put(eventType, index++);
        }
      }
    }
    return unmodifiableMap(map);
  }

  private static SortedSet<MethodDescriptor> getSortedMethods(final Class<?> type) {
    SortedSet<MethodDescriptor> methods = new TreeSet<MethodDescriptor>(METHOD_COMPARATOR);

    if (type == null) {
      return methods;
    }

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

  private static SortedSet<Class<?>> getSortedTypes(Class<?>[] types) {
    final TreeSet<Class<?>> classes = new TreeSet<Class<?>>(CLASS_COMPARATOR);
    if (types != null) {
      classes.addAll(asList(types));
    }
    return classes;
  }
}
