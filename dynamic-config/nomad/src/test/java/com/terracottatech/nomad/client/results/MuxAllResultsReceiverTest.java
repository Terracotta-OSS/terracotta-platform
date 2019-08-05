/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.client.results;

import com.terracottatech.nomad.client.Consistency;
import org.junit.Test;

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
