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
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * @author Alex Snaps
 */
public class CommonProxyFactory {
  public static final Comparator<Method> METHOD_COMPARATOR = new Comparator<Method>() {
    public int compare(final Method m1, final Method m2) {
      return m1.toGenericString().compareTo(m2.toGenericString());
    }
  };

  public static SortedSet<Method> getSortedMethods(final Class type) {
    SortedSet<Method> methods = new TreeSet<Method>(METHOD_COMPARATOR);

    final Method[] declaredMethods = type.getDeclaredMethods();

    if (declaredMethods.length > 256) {
      throw new IllegalArgumentException("Can't proxy that many methods on a single instance!");
    }

    for (Method method : declaredMethods) {
      if (!methods.add(method)) {
        throw new AssertionError("Ouch... looks like that didn't work!");
      }
    }
    return methods;
  }
}
