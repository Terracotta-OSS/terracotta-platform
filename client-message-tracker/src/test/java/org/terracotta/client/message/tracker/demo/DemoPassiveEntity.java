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

import org.terracotta.client.message.tracker.OOOMessageHandler;
import org.terracotta.client.message.tracker.OOOMessageHandlerConfiguration;
import org.terracotta.entity.ClientSourceId;
import org.terracotta.entity.ConfigurationException;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.EntityUserException;
import org.terracotta.entity.InvokeContext;
import org.terracotta.entity.PassiveServerEntity;
import org.terracotta.entity.ServiceException;
import org.terracotta.entity.ServiceRegistry;

public class DemoPassiveEntity implements PassiveServerEntity {

  private final OOOMessageHandler<EntityMessage, EntityResponse> messageHandler;

  public DemoPassiveEntity(ServiceRegistry serviceRegistry) throws ServiceException {
    OOOMessageHandlerConfiguration<EntityMessage, EntityResponse> messageHandlerConfiguration =
        new OOOMessageHandlerConfiguration<>("foo", msg -> msg instanceof TrackableMessage, 1, m -> 0);
    messageHandler = serviceRegistry.getService(messageHandlerConfiguration);
  }

  @Override
  public void invokePassive(InvokeContext context, EntityMessage message) throws EntityUserException {
    InvokeContext realContext;
    if (message instanceof DeferredMessage) {
      DeferredMessage deferredMessage = (DeferredMessage) message;
      realContext = new InvokeContext() {

        @Override
        public ClientSourceId getClientSource() {
          return deferredMessage.getDeferredClientSource();
        }

        @Override
        public long getCurrentTransactionId() {
          return deferredMessage.getDeferredTransactionId();
        }

        @Override
        public long getOldestTransactionId() {
          return context.getCurrentTransactionId();
        }

        @Override
        public boolean isValidClientInformation() {
          return true;
        }

        @Override
        public ClientSourceId makeClientSourceId(long l) {
          return null;
        }

        @Override
        public int getConcurrencyKey() {
          return 0;
        }
      };
    } else {
      realContext = context;
    }

    messageHandler.invoke(realContext, message, this::processMessage);
  }

  private EntityResponse processMessage(InvokeContext context, EntityMessage message) {
    if (message instanceof MessageTrackerSyncMessage) {
      MessageTrackerSyncMessage trackerSyncMessage = (MessageTrackerSyncMessage) message;
      messageHandler.loadTrackedResponsesForSegment(trackerSyncMessage.getSegmentIndex(), trackerSyncMessage.getClientSourceId(),
          trackerSyncMessage.getTrackedResponses());
    } else {
      // entity message handling logic
    }

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
  public void notifyDestroyed(ClientSourceId sourceId) {
    messageHandler.untrackClient(sourceId);
  }

  @Override
  public void createNew() throws ConfigurationException {

  }

  @Override
  public void destroy() {

  }
}
