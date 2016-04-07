package org.terracotta.clientcommunicator.support;

import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.MessageCodecException;

/**
 * @author vmad
 */
public interface ClientCommunicatorMessageFactory<M extends EntityMessage, R extends EntityResponse> {
    /**
     *
     * @param message
     * @return
     */
    M createEntityMessage(byte[] message) throws MessageCodecException;

    /**
     *
     * @param entityMessage
     * @return
     */
    byte[] extractBytesFromMessage(M entityMessage) throws MessageCodecException;

    /**
     *
     * @param message
     * @return
     */
    R createEntityResponse(byte[] message) throws MessageCodecException;

    /**
     *
     * @param entityResponse
     * @return
     */
    byte[] extractBytesFromResponse(R entityResponse) throws MessageCodecException;
}
