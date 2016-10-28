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

import java.nio.ByteBuffer;

/**
 * @author vmad
 */
class ClientCommunicatorRequestCodec {
    public static byte[] serialize(ClientCommunicatorRequest request) {
        int size = request.getMsgBytes().length + 2 * 4; // Integer size
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
