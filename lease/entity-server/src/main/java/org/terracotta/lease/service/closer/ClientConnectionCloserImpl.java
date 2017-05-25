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
package org.terracotta.lease.service.closer;

import org.terracotta.entity.ClientCommunicator;
import org.terracotta.entity.ClientDescriptor;

/**
 * The standard implementation of ClientConnectionCloser that uses the closeClientConnection() method on
 * ClientCommunicator to do the connection close. Note that ClientCommunicator closes the client connection
 * asynchronously.
 */
public class ClientConnectionCloserImpl implements ClientConnectionCloser {
  private final ClientCommunicator clientCommunicator;

  public ClientConnectionCloserImpl(ClientCommunicator clientCommunicator) {
    this.clientCommunicator = clientCommunicator;
  }

  @Override
  public void closeClientConnection(ClientDescriptor clientDescriptor) {
    clientCommunicator.closeClientConnection(clientDescriptor);
  }
}
