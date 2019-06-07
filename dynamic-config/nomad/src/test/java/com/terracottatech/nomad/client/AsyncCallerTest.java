/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.client;

import com.terracottatech.nomad.server.NomadException;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class AsyncCallerTest {
  private AtomicReference<String> success = new AtomicReference<>();
  private AtomicReference<Throwable> failure = new AtomicReference<>();

  @Before
  public void before() {
    success = new AtomicReference<>();
    failure = new AtomicReference<>();
  }

  @Test(timeout = 10_000L)
  public void success() throws Exception {
    try (AsyncCaller asyncCaller = new AsyncCaller(2, Duration.ofSeconds(20))) {
      runTimedAsync(asyncCaller, () -> "result");

      assertEquals("result", success.get());
      assertNull(failure.get());
    }
  }

  @Test(timeout = 10_000L)
  public void failure() throws Exception {
    try (AsyncCaller asyncCaller = new AsyncCaller(2, Duration.ofSeconds(20))) {
      runTimedAsync(asyncCaller, () -> {
        throw new NomadException();
      });

      assertNull(success.get());
      assertEquals(NomadException.class, failure.get().getClass());
    }
  }

  @Test(timeout = 10_000L)
  public void timeout() throws Exception {
    try (AsyncCaller asyncCaller = new AsyncCaller(2, Duration.ofMillis(10))) {
      runTimedAsync(asyncCaller, () -> {
        Thread.sleep(20_000);
        return "result";
      });

      assertNull(success.get());
      assertEquals(TimeoutException.class, failure.get().getClass());
    }
  }

  private void runTimedAsync(AsyncCaller asyncCaller, Callable<String> callable) throws Exception {
    Future<Void> future = asyncCaller.runTimedAsync(callable, success::set, failure::set);
    future.get();
  }
}
