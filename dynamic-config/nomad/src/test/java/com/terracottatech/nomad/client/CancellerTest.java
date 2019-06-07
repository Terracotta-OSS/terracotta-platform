/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.client;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.concurrent.Future;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class CancellerTest {
  @Mock
  private Future<String> future;

  @Test
  public void setFutureNoCancel() {
    Canceller<String> canceller = new Canceller<>();
    canceller.set(future);

    verifyNoMoreInteractions(future);
  }

  @Test
  public void setFutureThenCancelInterrupt() {
    Canceller<String> canceller = new Canceller<>();
    canceller.set(future);
    canceller.cancel(true);

    verify(future).cancel(true);
  }

  @Test
  public void setFutureThenCancelNoInterrupt() {
    Canceller<String> canceller = new Canceller<>();
    canceller.set(future);
    canceller.cancel(false);

    verify(future).cancel(false);
  }

  @Test
  public void cancelThenSetFutureInterrupt() {
    Canceller<String> canceller = new Canceller<>();
    canceller.cancel(true);
    canceller.set(future);

    verify(future).cancel(true);
  }

  @Test
  public void cancelThenSetFutureNoInterrupt() {
    Canceller<String> canceller = new Canceller<>();
    canceller.cancel(false);
    canceller.set(future);

    verify(future).cancel(false);
  }
}
