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
package org.terracotta.client.message.tracker.demo;

import org.terracotta.client.message.tracker.ClientMessageTracker;
import org.terracotta.client.message.tracker.ClientMessageTrackerConfiguration;
import org.terracotta.client.message.tracker.MessageTracker;
import org.terracotta.entity.ActiveServerEntity;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.ConfigurationException;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.EntityUserException;
import org.terracotta.entity.InvokeContext;
import org.terracotta.entity.PassiveSynchronizationChannel;
import org.terracotta.entity.ServiceException;
import org.terracotta.entity.ServiceRegistry;

public class DemoActiveEntity implements ActiveServerEntity {

  private final ClientMessageTracker clientMessageTracker;

  public DemoActiveEntity(ServiceRegistry serviceRegistry) throws ServiceException {
    ClientMessageTrackerConfiguration clientMessageTrackerConfiguration =
        new ClientMessageTrackerConfiguration("foo", new DummyTrackerPolicy());
    clientMessageTracker = serviceRegistry.getService(clientMessageTrackerConfiguration);
  }

  @Override
  public void connected(ClientDescriptor clientDescriptor) {

  }

  @Override
  public void disconnected(ClientDescriptor clientDescriptor) {
    clientMessageTracker.untrackClient(clientDescriptor);
  }

  @Override
  public EntityResponse invokeActive(InvokeContext context, EntityMessage message) throws EntityUserException {
    MessageTracker messageTracker = clientMessageTracker.getMessageTracker(context.getClientDescriptor());
    messageTracker.reconcile(context.getOldestTransactionId());
    EntityResponse response = messageTracker.getTrackedResponse(context.getCurrentTransactionId());
    if (response != null) {
      return response;
    }

    response = processMessage(message);
    messageTracker.track(context.getCurrentTransactionId(), message, response);
    return response;
  }

  private EntityResponse processMessage(EntityMessage message) {
    return null;
  }

  @Override
  public void createNew() throws ConfigurationException {
  }

  @Override
  public void loadExisting() {
  }

  @Override
  public void handleReconnect(ClientDescriptor clientDescriptor, byte[] bytes) {
    //no-op
  }

  @Override
  public void synchronizeKeyToPassive(PassiveSynchronizationChannel passiveSynchronizationChannel, int i) {
    //no-op
  }

  @Override
  public void destroy() {
    //no-op
  }
}
