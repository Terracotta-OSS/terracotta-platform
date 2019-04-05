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
package org.terracotta.client.message.tracker;

import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.EntityUserException;
import org.terracotta.entity.InvokeContext;
import org.terracotta.entity.StateDumpCollector;

import java.util.function.BiFunction;

public class OOOMessageHandlerImpl<M extends EntityMessage, R extends EntityResponse> implements OOOMessageHandler<M, R> {

  private final ClientMessageTracker<M, R> clientMessageTracker;
  private final DestroyCallback callback;

  public OOOMessageHandlerImpl(TrackerPolicy policy, DestroyCallback callback) {
    this.clientMessageTracker = new ClientMessageTrackerImpl(policy);
    this.callback = callback;
  }

  @Override
  public R invoke(InvokeContext context, M message, BiFunction<InvokeContext, M, R> invokeFunction) throws EntityUserException {
    MessageTracker<M, R> messageTracker = clientMessageTracker.getMessageTracker(context.getClientDescriptor());
    messageTracker.reconcile(context.getOldestTransactionId());
    R response = messageTracker.getTrackedResponse(context.getCurrentTransactionId());
    if (response != null) {
      return response;
    }

    response = invokeFunction.apply(context, message);
    messageTracker.track(context.getCurrentTransactionId(), message, response);
    return response;
  }

  @Override
  public void untrackClient(ClientDescriptor clientDescriptor) {
    clientMessageTracker.untrackClient(clientDescriptor);
  }

  @Override
  public void destroy() {
    this.callback.destroy();
  }

  @Override
  public void addStateTo(StateDumpCollector stateDumper) {
    clientMessageTracker.addStateTo(stateDumper);
  }
}
