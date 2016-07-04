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
