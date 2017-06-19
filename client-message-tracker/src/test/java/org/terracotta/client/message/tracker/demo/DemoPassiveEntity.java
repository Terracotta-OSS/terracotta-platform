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
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.ConfigurationException;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.EntityUserException;
import org.terracotta.entity.InvokeContext;
import org.terracotta.entity.PassiveServerEntity;
import org.terracotta.entity.ServiceException;
import org.terracotta.entity.ServiceRegistry;

public class DemoPassiveEntity implements PassiveServerEntity {

  private final ClientMessageTracker clientMessageTracker;

  public DemoPassiveEntity(ServiceRegistry serviceRegistry) throws ServiceException {
    ClientMessageTrackerConfiguration clientMessageTrackerConfiguration =
        new ClientMessageTrackerConfiguration("foo", new DummyTrackerPolicy());
    clientMessageTracker = serviceRegistry.getService(clientMessageTrackerConfiguration);
  }

  @Override
  public void invokePassive(InvokeContext context, EntityMessage message) throws EntityUserException {
    MessageTracker messageTracker = clientMessageTracker.getMessageTracker(context.getClientDescriptor());
    messageTracker.reconcile(context.getOldestTransactionId());
    EntityResponse response = messageTracker.getTrackedResponse(context.getCurrentTransactionId());
    if (response == null) {
      response = processMessage(message);
      messageTracker.track(context.getCurrentTransactionId(), message, response);
    }
  }

  private EntityResponse processMessage(EntityMessage message) {
    return null;
  }

  @Override
  public void startSyncEntity() {

  }

  @Override
  public void endSyncEntity() {

  }

  @Override
  public void startSyncConcurrencyKey(int i) {

  }

  @Override
  public void endSyncConcurrencyKey(int i) {

  }

  @Override
  public void notifyClientDisconnectedFromActive(ClientDescriptor client) {
    clientMessageTracker.untrackClient(client);
  }

  @Override
  public void createNew() throws ConfigurationException {

  }

  @Override
  public void destroy() {

  }
}
