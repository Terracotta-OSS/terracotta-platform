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

import java.util.stream.Collectors;
import org.terracotta.client.message.tracker.OOOMessageHandler;
import org.terracotta.client.message.tracker.OOOMessageHandlerConfiguration;
import org.terracotta.entity.ActiveInvokeContext;
import org.terracotta.entity.ActiveServerEntity;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.ClientSourceId;
import org.terracotta.entity.ConfigurationException;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.EntityUserException;
import org.terracotta.entity.InvokeContext;
import org.terracotta.entity.PassiveSynchronizationChannel;
import org.terracotta.entity.ServiceException;
import org.terracotta.entity.ServiceRegistry;

import java.util.stream.Stream;
import org.terracotta.client.message.tracker.RecordedMessage;

public class DemoActiveEntity implements ActiveServerEntity<EntityMessage, EntityResponse> {

  private final OOOMessageHandler<EntityMessage, EntityResponse> messageHandler;

  public DemoActiveEntity(ServiceRegistry serviceRegistry) throws ServiceException {
    OOOMessageHandlerConfiguration<EntityMessage, EntityResponse> messageHandlerConfiguration =
        new OOOMessageHandlerConfiguration<>("foo", msg -> msg instanceof TrackableMessage);
    messageHandler = serviceRegistry.getService(messageHandlerConfiguration);
  }

  @Override
  public void connected(ClientDescriptor clientDescriptor) {
    //no-op
  }

  @Override
  public void disconnected(ClientDescriptor clientDescriptor) {
    //no-op
  }

  @Override
  public void notifyDestroyed(ClientSourceId sourceId) {
    messageHandler.untrackClient(sourceId);
  }

  @Override
  public EntityResponse invokeActive(ActiveInvokeContext<EntityResponse> context, EntityMessage message) throws EntityUserException {
    return messageHandler.invoke(context, message, this::processMessage);
  }

  private EntityResponse processMessage(InvokeContext context, EntityMessage message) {
    return null;
  }

  @Override
  public void createNew() throws ConfigurationException {
  }

  @Override
  public void loadExisting() {
  }

  @Override
  public ActiveServerEntity.ReconnectHandler startReconnect() {
    return (ClientDescriptor clientDescriptor, byte[] bytes)->{
    //no-op
    };
  }

  @SuppressWarnings("deprecation")
  @Override
  public void synchronizeKeyToPassive(PassiveSynchronizationChannel<EntityMessage> passiveSynchronizationChannel, int concurrencyKey) {
    // Sync entity data for the given concurrency key
    EntityMessage entityDataSyncMessage = null;
    passiveSynchronizationChannel.synchronizeToPassive(entityDataSyncMessage);

    // Sync client message tracker state
    int segmentIndex = concurrencyKeyToSegmentIndex(concurrencyKey);
    messageHandler.getTrackedClients().forEach(clientSourceId -> {
      Stream<RecordedMessage<EntityMessage, EntityResponse>> trackedResponsesForSegment = messageHandler.getRecordedMessages();
      EntityMessage clientMessageTrackerSegmentData = new MessageTrackerSyncMessage(trackedResponsesForSegment.collect(Collectors.toList()));
      passiveSynchronizationChannel.synchronizeToPassive(clientMessageTrackerSegmentData);
    });
  }

  private int concurrencyKeyToSegmentIndex(int concurrencyKey) {
    return -1;  //Do the proper transformation
  }

  @Override
  public void destroy() {
    //no-op
  }
}
