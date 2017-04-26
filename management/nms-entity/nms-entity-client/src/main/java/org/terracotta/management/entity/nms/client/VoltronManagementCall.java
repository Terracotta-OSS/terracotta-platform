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
package org.terracotta.management.entity.nms.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.model.context.Context;

import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 * @author Mathieu Carbou
 */
public class VoltronManagementCall<T> implements ManagementCall<T> {

  private static final Logger LOGGER = LoggerFactory.getLogger(VoltronManagementCall.class);

  private final CompletableFuture<T> future = new CompletableFuture<>();
  private final Context target;
  private final String managementCallId;
  private final Class<T> returnType;
  private final long timeout;
  private final Consumer<VoltronManagementCall<?>> onDone;

  public VoltronManagementCall(String managementCallId, Context target, Class<T> returnType, long timeout, Consumer<VoltronManagementCall<?>> onDone) {
    this.target = Objects.requireNonNull(target);
    this.managementCallId = Objects.requireNonNull(managementCallId);
    this.returnType = Objects.requireNonNull(returnType);
    this.timeout = timeout;
    this.onDone = Objects.requireNonNull(onDone);
  }

  @Override
  public T waitForReturn() throws TimeoutException, ExecutionException, InterruptedException, CancellationException, IllegalManagementCallException {
    try {
      return future.get(timeout, TimeUnit.MILLISECONDS);
    } catch (TimeoutException e) {
      // if a management call times out as per the config timeout, then we cancel the call.
      cancel();
      throw e;
    } catch (ExecutionException e) {
      if (e.getCause() instanceof IllegalManagementCallException) {
        throw (IllegalManagementCallException) e.getCause();
      }
      throw e;
    }
  }

  @Override
  public Class<T> getReturnType() {
    return returnType;
  }

  @Override
  public Context getTarget() {
    return target;
  }

  @Override
  public String getId() {
    return managementCallId;
  }

  @Override
  public boolean isCanceled() {
    return future.isCancelled();
  }

  @Override
  public CompletionStage<T> asCompletionStage() {
    return future;
  }

  @Override
  public void cancel() {
    if (!future.isDone()) {
      LOGGER.trace("[{}] cancel()", managementCallId);
      future.cancel(true);
      onDone.accept(this);
    }
  }

  void completeExceptionally(Throwable throwable) {
    if (!future.isDone()) {
      LOGGER.trace("[{}] completeExceptionally({})", managementCallId, throwable.getClass().getName());
      future.completeExceptionally(throwable);
      onDone.accept(this);
    }
  }

  void complete(T value) {
    if (!future.isDone()) {
      LOGGER.trace("[{}] complete()", managementCallId);
      future.complete(value);
      onDone.accept(this);
    }
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("ManagementCall{");
    sb.append("target=").append(target);
    sb.append(", managementCallId='").append(managementCallId).append('\'');
    sb.append(", returnType=").append(returnType);
    sb.append('}');
    return sb.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    VoltronManagementCall<?> that = (VoltronManagementCall<?>) o;
    return managementCallId.equals(that.managementCallId);
  }

  @Override
  public int hashCode() {
    return managementCallId.hashCode();
  }
}
