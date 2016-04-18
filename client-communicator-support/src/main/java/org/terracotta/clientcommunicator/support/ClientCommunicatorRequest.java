package org.terracotta.clientcommunicator.support;

/**
 * @author vmad
 */
class ClientCommunicatorRequest {
    private final ClientCommunicatorRequestType requestType;
    private final int requestSequenceNumber;
    private final byte[] msgBytes;

    public ClientCommunicatorRequest(ClientCommunicatorRequestType requestType, int requestSequenceNumber, byte[] msgBytes) {
        this.requestType = requestType;
        this.requestSequenceNumber = requestSequenceNumber;
        this.msgBytes = msgBytes;
    }

    public ClientCommunicatorRequestType getRequestType() {
        return requestType;
    }

    public byte[] getMsgBytes() {
        return msgBytes;
    }

    public int getRequestSequenceNumber() {
        return requestSequenceNumber;
    }
}
