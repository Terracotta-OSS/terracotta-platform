/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
package org.terracotta.nomad.client;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.UUID;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class AllResultsReceiverAdapterTest<U, V> {
  protected void runTest(Class<U> adaptedClass, Class<V> adapterClass) throws Exception {
    Method[] methods = adapterClass.getDeclaredMethods();
    for (Method method : methods) {
      if (Modifier.isPublic(method.getModifiers())) {
        runTest(adaptedClass, adapterClass, method);
      }
    }
  }

  private void runTest(Class<U> adaptedClass, Class<V> adapterClass, Method method) throws Exception {
    U underlying = mock(adaptedClass);
    V adapter = adapterClass.getConstructor(adaptedClass).newInstance(underlying);

    Class<?>[] parameterTypes = method.getParameterTypes();
    Object[] args = new Object[parameterTypes.length];

    for (int i = 0; i < parameterTypes.length; i++) {
      Class<?> parameterType = parameterTypes[i];

      if (parameterType.equals(String.class)) {
        args[i] = Integer.toString(i);
      } else if (parameterType.equals(UUID.class)) {
        args[i] = UUID.randomUUID();
      } else if (parameterType.equals(Consistency.class)) {
        args[i] = Consistency.CONSISTENT;
      } else {
        args[i] = mock(parameterType);
      }
    }

    Method underlyingMethod = null;
    boolean underlyingMethodExists = true;
    try {
      underlyingMethod = adaptedClass.getMethod(method.getName(), parameterTypes);
    } catch (NoSuchMethodException e) {
      underlyingMethodExists = false;
    }

    try {
      method.invoke(adapter, args);

      if (underlyingMethodExists) {
        underlyingMethod.invoke(verify(underlying), args);
      } else {
        fail("Expected AssertionError");
      }
    } catch (InvocationTargetException e) {
      if (underlyingMethodExists) {
        assertTrue(e.getCause() instanceof AssertionError);
      }
    }
  }
}
