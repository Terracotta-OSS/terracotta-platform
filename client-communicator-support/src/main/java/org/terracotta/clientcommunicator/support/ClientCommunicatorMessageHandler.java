package org.terracotta.clientcommunicator.support;

/**
 * @author vmad
 */
public interface ClientCommunicatorMessageHandler {
    /**
     *
     * @param message
     */
    void handleMessage(byte[] message);
}
