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
package org.terracotta.clientcommunicator.support;


import org.terracotta.entity.ClientCommunicator;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.MessageCodecException;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author vmad
 */
public class ClientCommunicatorServerManagerImpl<M extends EntityMessage, R extends EntityResponse> implements ClientCommunicatorServerManager<M, R> {

    private final ClientCommunicator clientCommunicator;
    private final ClientCommunicatorMessageFactory<M, R> clientCommunicatorMessageFactory;
    private final AtomicInteger requestSequence = new AtomicInteger(0);
    private final ConcurrentMap<Integer, ClientRequestInfo> pendingRequests = new ConcurrentHashMap<Integer, ClientRequestInfo>();

    public ClientCommunicatorServerManagerImpl(ClientCommunicator clientCommunicator, ClientCommunicatorMessageFactory<M, R> clientCommunicatorMessageFactory) {
        this.clientCommunicator = clientCommunicator;
        this.clientCommunicatorMessageFactory = clientCommunicatorMessageFactory;
    }

    @Override
    public R sendWithAck(Set<ClientDescriptor> toClients, byte[] message, ClientDescriptor source) throws MessageCodecException {
        int requestSequenceNumber = requestSequence.getAndIncrement();
        pendingRequests.putIfAbsent(requestSequenceNumber, new ClientRequestInfo(source, toClients));
        for (ClientDescriptor connectedClient : toClients) {
            clientCommunicator.sendNoResponse(connectedClient,
                    clientCommunicatorMessageFactory.createEntityResponse(ClientCommunicatorRequestCodec.serialize(new ClientCommunicatorRequest(ClientCommunicatorRequestType.ACK, requestSequenceNumber, message))));
        }

        return clientCommunicatorMessageFactory.createEntityResponse(ClientCommunicatorRequestCodec.serialize(new ClientCommunicatorRequest(ClientCommunicatorRequestType.CLIENT_WAIT, requestSequenceNumber, new byte[0])));

    }

    @Override
    public void sendWithNoAck(Set<ClientDescriptor> toClients, byte[] message) throws MessageCodecException {
        for (ClientDescriptor connectedClient : toClients) {
            clientCommunicator.sendNoResponse(connectedClient,
                    clientCommunicatorMessageFactory.createEntityResponse(ClientCommunicatorRequestCodec.serialize(new ClientCommunicatorRequest(ClientCommunicatorRequestType.NO_ACK, -1, message))));
        }
    }

    @Override
    public void handleClientAck(ClientDescriptor client, M ackMessage) throws MessageCodecException {
        ByteBuffer buffer = ByteBuffer.wrap(clientCommunicatorMessageFactory.extractBytesFromMessage(ackMessage));
        int requestSequenceNumber = buffer.getInt();
        ClientRequestInfo clientRequestInfo = pendingRequests.get(requestSequenceNumber);
        if(clientRequestInfo != null) {
            clientRequestInfo.addAckForClient(client);
            if (clientRequestInfo.isAckCompleted()) {
                clientCommunicator.sendNoResponse(clientRequestInfo.getClientDescriptor(),
                        clientCommunicatorMessageFactory.createEntityResponse(ClientCommunicatorRequestCodec.serialize(new ClientCommunicatorRequest(ClientCommunicatorRequestType.REQUEST_COMPLETE,
                                requestSequenceNumber, new byte[0]))));

                pendingRequests.remove(requestSequenceNumber);
            }
        }
    }

    @Override
    public void handleClientDisconnect(ClientDescriptor client) {
        for(ClientRequestInfo requestInfo : pendingRequests.values()) {
            requestInfo.removeDisconnectedClient(client);
        }
    }


    private static class ClientRequestInfo {
        private final ClientDescriptor clientDescriptor;
        private final Set<ClientDescriptor> connectedClients;
        private final Set<ClientDescriptor> ackedClients = new HashSet<ClientDescriptor>();

        private ClientRequestInfo(ClientDescriptor clientDescriptor, Set<ClientDescriptor> connectedClients) {
            this.clientDescriptor = clientDescriptor;
            this.connectedClients = new HashSet<ClientDescriptor>(connectedClients);
        }

        public ClientDescriptor getClientDescriptor() {
            return clientDescriptor;
        }

        public void addAckForClient(ClientDescriptor client) {
            ackedClients.add(client);
        }

        public boolean isAckCompleted() {
            return ackedClients.equals(connectedClients);
        }

        public void removeDisconnectedClient(ClientDescriptor client) {
            connectedClients.remove(client);
        }
    }
}
