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
package org.terracotta.lease;

import org.terracotta.entity.ActiveInvokeContext;
import org.terracotta.entity.ActiveServerEntity;
import org.terracotta.entity.ClientCommunicator;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.ConfigurationException;
import org.terracotta.entity.IEntityMessenger;
import org.terracotta.entity.MessageCodecException;
import org.terracotta.entity.PassiveSynchronizationChannel;
import org.terracotta.lease.service.LeaseResult;
import org.terracotta.lease.service.LeaseService;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The active server-side entity for connection leasing. Pretty much just delegates to the LeaseService.
 */
class ActiveLeaseAcquirer implements ActiveServerEntity<LeaseMessage, LeaseResponse> {
  private final LeaseService leaseService;
  private final ClientCommunicator clientCommunicator;
  private final IEntityMessenger entityMessenger;
  private final ConcurrentHashMap<ClientDescriptor, Long> connectionSequenceNumbers = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<UUID, ClientDescriptor> clientDescriptors = new ConcurrentHashMap<>();

  ActiveLeaseAcquirer(LeaseService leaseService, ClientCommunicator clientCommunicator, IEntityMessenger entityMessenger) {
    this.leaseService = leaseService;
    this.clientCommunicator = clientCommunicator;
    this.entityMessenger = entityMessenger;
  }

  @Override
  public void connected(ClientDescriptor clientDescriptor) {
  }

  @Override
  public void disconnected(ClientDescriptor clientDescriptor) {
    leaseService.disconnected(clientDescriptor);
    connectionSequenceNumbers.remove(clientDescriptor);
  }

  @Override
  public LeaseResponse invokeActive(ActiveInvokeContext context, LeaseMessage leaseMessage) {
    LeaseMessageType messageType = leaseMessage.getType();
    switch (messageType) {
      case LEASE_REQUEST:
        return handleLeaseRequest(context, (LeaseRequest) leaseMessage);
      case LEASE_RECONNECT_FINISHED:
        return handleReconnectFinished((LeaseReconnectFinished) leaseMessage);
      default:
        throw new AssertionError("Unexpected type of LeaseMessage: " + messageType);
    }
  }

  private LeaseResponse handleLeaseRequest(ActiveInvokeContext context, LeaseRequest leaseRequest) {
    ClientDescriptor clientDescriptor = context.getClientDescriptor();

    if (!isLatestConnection(clientDescriptor, leaseRequest)) {
      return LeaseRequestResult.oldConnection();
    }

    LeaseResult leaseResult = leaseService.acquireLease(clientDescriptor);

    if (leaseResult.isLeaseGranted()) {
      long leaseLength = leaseResult.getLeaseLength();
      return LeaseRequestResult.leaseGranted(leaseLength);
    } else {
      return LeaseRequestResult.leaseNotGranted();
    }
  }

  private boolean isLatestConnection(ClientDescriptor clientDescriptor, LeaseRequest leaseRequest) {
    Long latestConnectionSequenceNumber = connectionSequenceNumbers.get(clientDescriptor);

    if (latestConnectionSequenceNumber == null) {
      return true;
    }

    long messageConnectionSequenceNumber = leaseRequest.getConnectionSequenceNumber();
    if (messageConnectionSequenceNumber > latestConnectionSequenceNumber) {
      throw new AssertionError("Connection sequence numbers should not jump ahead, expected: " + latestConnectionSequenceNumber + " actual: " + messageConnectionSequenceNumber);
    }

    return messageConnectionSequenceNumber == latestConnectionSequenceNumber;
  }

  @Override
  public void handleReconnect(ClientDescriptor clientDescriptor, byte[] bytes) {
    LeaseReconnectData reconnectData = LeaseReconnectData.decode(bytes);

    long connectionSequenceNumber = reconnectData.getConnectionSequenceNumber();
    connectionSequenceNumbers.put(clientDescriptor, connectionSequenceNumber);

    leaseService.reconnecting(clientDescriptor);

    // There's no way to encode a ClientDescriptor so use this UUID map
    UUID uuid = UUID.randomUUID();
    clientDescriptors.put(uuid, clientDescriptor);

    try {
      entityMessenger.messageSelf(new LeaseReconnectFinished(uuid));
    } catch (MessageCodecException e) {
      throw new RuntimeException("Failed to encode self message to indicate reconnect completion", e);
    }
  }

  private LeaseResponse handleReconnectFinished(LeaseReconnectFinished reconnectFinished) {
    UUID uuid = reconnectFinished.getUUID();
    ClientDescriptor clientDescriptor = clientDescriptors.remove(uuid);

    leaseService.reconnected(clientDescriptor);

    try {
      clientCommunicator.sendNoResponse(clientDescriptor, new LeaseAcquirerAvailable());
    } catch (MessageCodecException e) {
      throw new RuntimeException("Failed to encode message to client to inform that reconnect has completed", e);
    }

    // LeaseReconnectFinished is only sent via a self message
    return new IgnoredLeaseResponse();
  }

  @Override
  public void loadExisting() {
  }

  @Override
  public void createNew() throws ConfigurationException {
  }

  @Override
  public void synchronizeKeyToPassive(PassiveSynchronizationChannel<LeaseMessage> passiveSynchronizationChannel, int concurrencyKey) {
  }

  @Override
  public void destroy() {
  }
}
