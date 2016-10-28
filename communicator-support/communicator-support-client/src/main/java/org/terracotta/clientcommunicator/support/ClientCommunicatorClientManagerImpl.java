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

import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.MessageCodecException;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.terracotta.clientcommunicator.support.ClientCommunicatorRequestType.ACK;
import static org.terracotta.clientcommunicator.support.ClientCommunicatorRequestType.NO_ACK;
import static org.terracotta.clientcommunicator.support.ClientCommunicatorRequestType.REQUEST_COMPLETE;

/**
 * @author vmad
 */
public class ClientCommunicatorClientManagerImpl<M extends EntityMessage, R extends EntityResponse> implements ClientCommunicatorClientManager<M, R> {

    private final EntityClientEndpoint<M, R> entityClientEndpoint;
    private final ClientCommunicatorMessageFactory<M, R> clientCommunicatorMessageFactory;
    private final ConcurrentMap<Integer, AtomicBoolean> monitors = new ConcurrentHashMap<Integer, AtomicBoolean>();

    public ClientCommunicatorClientManagerImpl(EntityClientEndpoint<M, R> entityClientEndpoint, ClientCommunicatorMessageFactory<M, R> clientCommunicatorMessageFactory) {
        this.entityClientEndpoint = entityClientEndpoint;
        this.clientCommunicatorMessageFactory = clientCommunicatorMessageFactory;
    }

    @Override
    public void handleInvokeResponse(R response) {
        try {
            ClientCommunicatorRequest clientCommunicatorRequest = ClientCommunicatorRequestCodec.deserialize(clientCommunicatorMessageFactory.extractBytesFromResponse(response));

            int requestSequenceNumber = clientCommunicatorRequest.getRequestSequenceNumber();
            if(clientCommunicatorRequest.getRequestType() != ClientCommunicatorRequestType.CLIENT_WAIT) {
                throw new RuntimeException("Received Wrong ClientCommunicatorRequestType in invokeResponse: expected - " + ClientCommunicatorRequestType.CLIENT_WAIT + ", got - " + clientCommunicatorRequest.getRequestType());
            }
            AtomicBoolean last = monitors.putIfAbsent(requestSequenceNumber, new AtomicBoolean());
            if(last == null) {
                AtomicBoolean monitor = monitors.get(requestSequenceNumber);
                while (!monitor.get()) {
                    synchronized (monitor) {
                        monitor.wait();
                    }
                }
            }
            monitors.remove(requestSequenceNumber);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (MessageCodecException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void handleClientCommunicatorMessage(R message, ClientCommunicatorMessageHandler clientCommunicatorMessageHandler) {
      try {
        ClientCommunicatorRequest clientCommunicatorRequest = ClientCommunicatorRequestCodec.deserialize(clientCommunicatorMessageFactory.extractBytesFromResponse(message));
        switch (clientCommunicatorRequest.getRequestType()) {
            case ACK:
                clientCommunicatorMessageHandler.handleMessage(clientCommunicatorRequest.getMsgBytes());
                try {
                    entityClientEndpoint.beginInvoke().message(clientCommunicatorMessageFactory.createEntityMessage(ByteBuffer.allocate(4).putInt(clientCommunicatorRequest.getRequestSequenceNumber()).array())).invoke();
                } catch (MessageCodecException e) {
                    throw new RuntimeException(e);
                }
                break;

            case NO_ACK:
                clientCommunicatorMessageHandler.handleMessage(clientCommunicatorRequest.getMsgBytes());
                break;

            case REQUEST_COMPLETE:
                int requestSequenceNumber = clientCommunicatorRequest.getRequestSequenceNumber();
                AtomicBoolean last = monitors.putIfAbsent(requestSequenceNumber, new AtomicBoolean());
                if(last != null) {
                    AtomicBoolean monitor = monitors.get(requestSequenceNumber);
                    synchronized (monitor) {
                        monitor.compareAndSet(false, true);
                        monitor.notifyAll();
                    }
                }
                break;

            default:
                throw new IllegalArgumentException("unexpected/unknown ClientCommunicatorRequestType: " + clientCommunicatorRequest.getRequestType());
        }
      } catch (MessageCodecException e) {
        // This would mean a serious bug in the message factory.
        throw new RuntimeException(e);
      }
    }
}

