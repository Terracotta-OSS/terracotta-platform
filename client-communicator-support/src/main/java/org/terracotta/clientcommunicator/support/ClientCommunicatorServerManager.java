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

import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.MessageCodecException;

import java.util.Set;

/**
 *
 * ClientCommunicatorServerManager and ClientCommunicatorClientManager provides support for sending messages from server to
 * client with acks (with client-side waiting). Note that the current api ({@link org.terracotta.entity.ClientCommunicator#send(ClientDescriptor, EntityResponse)}
 * for achieving this (with server-side waiting) is deprecated
 *
 *
 * @author vmad
 */
public interface ClientCommunicatorServerManager<M extends EntityMessage, R extends EntityResponse> {
    /**
     *
     * Sends a message to given set of clients and expects client acks on request invoke path
     *
     * Note that Entity should send returned {@link EntityResponse} to the source client and client-side entity should call
     * {@link ClientCommunicatorClientManager#handleInvokeResponse(EntityResponse)} with this response so that client-side
     * waiting can be achieved
     *
     * Note that Entity should call {@link #handleClientAck} with client ack when it receives
     *
     * @param toClients Set of clients to which given message will be sent
     * @param message   the message to be send
     * @param source    the client which initiated current Entity request
     * @return a {@link EntityResponse}
     * @throws MessageCodecException
     *
     * @see ClientCommunicatorClientManager
     */
    R sendWithAck(Set<ClientDescriptor> toClients, byte[] message, ClientDescriptor source) throws MessageCodecException;

    /**
     * Sends a message to given set of clients
     *
     * @param toClients Set of clients to which given message will be sent
     * @param message  the message to be send
     * @throws MessageCodecException
     */
    void sendWithNoAck(Set<ClientDescriptor> toClients, byte[] message) throws MessageCodecException;

    /**
     * Handles client acks for messages that were sent using {@link #sendWithAck(Set, byte[], ClientDescriptor)}
     *
     * @param client
     * @param ackMessage
     * @throws MessageCodecException
     */
    void handleClientAck(ClientDescriptor client, M ackMessage) throws MessageCodecException;

    /**
     * Handles client disconnects
     *
     * Note that entity should call this whenever there is a client disconnect
     *
     * @param client the disconnected client
     */
    void handleClientDisconnect(ClientDescriptor client);
}
