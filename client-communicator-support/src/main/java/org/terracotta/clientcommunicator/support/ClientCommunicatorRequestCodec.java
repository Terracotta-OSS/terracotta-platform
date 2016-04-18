package org.terracotta.clientcommunicator.support;

import java.nio.ByteBuffer;

/**
 * @author vmad
 */
class ClientCommunicatorRequestCodec {
    public static byte[] serialize(ClientCommunicatorRequest request) {
        int size = request.getMsgBytes().length + 12;
        ByteBuffer buffer = ByteBuffer.allocate(size).
                putInt(request.getRequestType().ordinal()).
                putInt(request.getRequestSequenceNumber()).
                put(request.getMsgBytes());
        return buffer.array();
    }
    public static ClientCommunicatorRequest deserialize(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        ClientCommunicatorRequestType requestType = ClientCommunicatorRequestType.values()[buffer.getInt()];
        int requestSequenceNumber = buffer.getInt();
        byte[] msgBytes = new byte[buffer.remaining()];
        buffer.get(msgBytes);
        return new ClientCommunicatorRequest(requestType, requestSequenceNumber, msgBytes);
    }
}
