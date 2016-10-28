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

import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;

import java.util.Set;

/**
 *  @see ClientCommunicatorServerManager
 *
 * @author vmad
 */
public interface ClientCommunicatorClientManager<M extends EntityMessage, R extends EntityResponse> {
    /**
     *
     * Handles the response that was received from {@link ClientCommunicatorServerManager#sendWithAck(Set, byte[], ClientDescriptor)}
     *
     * @param response the received response
     */
    void handleInvokeResponse(R response);

    /**
     * Handles messages received from server using {@link ClientCommunicatorServerManager#sendWithAck(Set, byte[], ClientDescriptor)}
     *
     * @param message the received message
     * @param clientCommunicatorMessageHandler an entity handler to process server message
     */
    void handleClientCommunicatorMessage(R message, ClientCommunicatorMessageHandler clientCommunicatorMessageHandler);
}
