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

import org.junit.Assert;
import org.junit.Test;

/**
 * @author vmad
 */
public class ClientCommunicatorRequestCodecTest {

    private static final String TEST_MESSAGE = "TestMessage";
    private static final int TEST_SEQUENCE_NUMBER = 1;
    private static final ClientCommunicatorRequestType TEST_REQUEST_TYPE = ClientCommunicatorRequestType.ACK;

    @Test
    public void testCodec() throws Exception {
        byte[] serialized = ClientCommunicatorRequestCodec.serialize(
                new ClientCommunicatorRequest(TEST_REQUEST_TYPE, TEST_SEQUENCE_NUMBER, TEST_MESSAGE.getBytes()));
        ClientCommunicatorRequest deserializedRequest = ClientCommunicatorRequestCodec.deserialize(serialized);

        Assert.assertEquals(TEST_REQUEST_TYPE, deserializedRequest.getRequestType());
        Assert.assertEquals(TEST_SEQUENCE_NUMBER, deserializedRequest.getRequestSequenceNumber());
        Assert.assertArrayEquals(TEST_MESSAGE.getBytes(), deserializedRequest.getMsgBytes());
    }
}
