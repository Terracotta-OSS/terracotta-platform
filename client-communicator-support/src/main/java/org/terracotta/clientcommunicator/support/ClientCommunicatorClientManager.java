package org.terracotta.clientcommunicator.support;

import org.terracotta.entity.ClientDescriptor;
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
    void handleClientCommunicatorMessage(byte[] message, ClientCommunicatorMessageHandler clientCommunicatorMessageHandler);
}
