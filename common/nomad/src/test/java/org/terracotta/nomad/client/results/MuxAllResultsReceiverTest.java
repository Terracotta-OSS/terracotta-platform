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
package org.terracotta.nomad.client.results;

import org.junit.Test;
import org.terracotta.nomad.client.Consistency;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class MuxAllResultsReceiverTest {
  @Test
  public void allMethodsDelegate() throws Exception {
    Method[] methods = AllResultsReceiver.class.getMethods();
    for (Method method : methods) {
      runTest(method);
    }
  }

  @SuppressWarnings("unchecked")
  private void runTest(Method method) throws Exception {
    AllResultsReceiver<String> underlying1 = mock(AllResultsReceiver.class);
    AllResultsReceiver<String> underlying2 = mock(AllResultsReceiver.class);
    MuxAllResultsReceiver<String> mux = new MuxAllResultsReceiver<>(Arrays.asList(underlying1, underlying2));

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

    method.invoke(mux, args);

    Method underlyingMethod = AllResultsReceiver.class.getMethod(method.getName(), parameterTypes);
    underlyingMethod.invoke(verify(underlying1), args);
    underlyingMethod.invoke(verify(underlying2), args);
  }
}
