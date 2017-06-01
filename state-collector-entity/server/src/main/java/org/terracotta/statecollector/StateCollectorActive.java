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
package org.terracotta.statecollector;

import org.terracotta.entity.ActiveServerEntity;
import org.terracotta.entity.ClientCommunicator;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.ConfigurationException;
import org.terracotta.entity.EntityUserException;
import org.terracotta.entity.MessageCodecException;
import org.terracotta.entity.PassiveSynchronizationChannel;
import org.terracotta.entity.StateDumpCollector;
import org.terracotta.entity.StateDumpable;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

class StateCollectorActive implements ActiveServerEntity<StateCollectorMessage, StateCollectorMessage>, StateDumpable {
  
  private final Set<ClientDescriptor> connectedClients = ConcurrentHashMap.newKeySet();
  private final ClientCommunicator clientCommunicator;

  public StateCollectorActive(ClientCommunicator clientCommunicator) {
    this.clientCommunicator = clientCommunicator;
  }

  @Override
  public void connected(ClientDescriptor clientDescriptor) {
    connectedClients.add(clientDescriptor);
  }

  @Override
  public void disconnected(ClientDescriptor clientDescriptor) {
    connectedClients.remove(clientDescriptor);
  }

  @Override
  public StateCollectorMessage invoke(final ClientDescriptor clientDescriptor, final StateCollectorMessage stateCollectorMessage) throws EntityUserException {
    //no invokes from client
    return null;
  }

  @Override
  public void loadExisting() {
    //Nothing to do
  }

  @Override
  public void handleReconnect(final ClientDescriptor clientDescriptor, final byte[] bytes) {
    //Nothing to do
  }

  @Override
  public void synchronizeKeyToPassive(final PassiveSynchronizationChannel<StateCollectorMessage> passiveSynchronizationChannel, final int i) {
    //Nothing to do
  }

  @Override
  public void createNew() throws ConfigurationException {
    //Nothing to do
  }

  @Override
  public void destroy() {
    connectedClients.clear();
  }

  @Override
  public void addStateTo(StateDumpCollector stateDumper) {
    for (ClientDescriptor connectedClient : connectedClients) {
      try {
        clientCommunicator.sendNoResponse(connectedClient, new StateCollectorMessage(StateCollectorMessageType.DUMP));
      } catch (MessageCodecException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
