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
package org.terracotta.voltron.proxy.server;

import org.terracotta.entity.ActiveInvokeContext;
import org.terracotta.entity.ActiveServerEntity;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.EntityUserException;
import org.terracotta.entity.InvokeContext;
import org.terracotta.entity.PassiveSynchronizationChannel;
import org.terracotta.entity.StateDumpCollector;
import org.terracotta.voltron.proxy.Codec;
import org.terracotta.voltron.proxy.ProxyEntityMessage;
import org.terracotta.voltron.proxy.ProxyEntityResponse;

import java.util.Collection;

/**
 * @author Alex Snaps
 */
public abstract class ActiveProxiedServerEntity<S, R, M extends Messenger> implements ActiveServerEntity<ProxyEntityMessage, ProxyEntityResponse> {

  private final ProxyInvoker<?> entityInvoker = new ProxyInvoker<>(this);

  private S synchronizer;
  private M messenger;
  private Codec codec;
  private Class<R> reconnectDataType;

  @Override
  public ProxyEntityResponse invokeActive(ActiveInvokeContext context, ProxyEntityMessage message) throws EntityUserException {
    switch (message.getType()) {
      case MESSAGE:
      case MESSENGER:
        return entityInvoker.invoke(context, message);
      default:
        throw new AssertionError(message.getType());
    }
  }

  @Override
  public void connected(ClientDescriptor clientDescriptor) {
    entityInvoker.addClient(clientDescriptor);
  }

  @Override
  public void disconnected(ClientDescriptor clientDescriptor) {
    entityInvoker.removeClient(clientDescriptor);
  }

  @Override
  public final void handleReconnect(final ClientDescriptor clientDescriptor, final byte[] extendedReconnectData) {
    if (reconnectDataType != null && codec != null) {
      R state = null;
      if (extendedReconnectData != null && extendedReconnectData.length > 0) {
        state = codec.decode(reconnectDataType, extendedReconnectData);

      }
      onReconnect(clientDescriptor, state);
    }
  }

  @Override
  public final void synchronizeKeyToPassive(final PassiveSynchronizationChannel<ProxyEntityMessage> channel, final int concurrencyKey) {
    if (synchronizer != null) {
      SyncProxyFactory.setCurrentChannel(channel);
      try {
        synchronizeKeyToPassive(concurrencyKey);
      } finally {
        SyncProxyFactory.removeCurrentChannel();
      }
    }
  }

  @Override
  public void createNew() {
    // Don't care I think
  }

  @Override
  public void loadExisting() {
    // Don't care I think
  }

  @Override
  public void destroy() {
    if (messenger != null) {
      messenger.unSchedule();
    }
  }

  @Override
  public final void addStateTo(StateDumpCollector stateDumpCollector) {
    stateDumpCollector.addState("instance", this.toString());
    // clients, by default for all active entities
    Collection<ClientDescriptor> clients = getClients();
    stateDumpCollector.addState("clientCount", String.valueOf(clients.size()));
    stateDumpCollector.addState("clients",clients);

    // custom
    dumpState(stateDumpCollector);
  }

  protected void dumpState(StateDumpCollector dump) {
  }

  protected void synchronizeKeyToPassive(int concurrencyKey) {
  }

  protected void onReconnect(ClientDescriptor clientDescriptor, R state) {
  }

  protected final <M> void fireMessage(Class<M> type, M message, boolean echo) {entityInvoker.fireMessage(type, message, echo);}

  protected final <M> void fireMessage(Class<M> type, M message, ClientDescriptor... clients) {entityInvoker.fireMessage(type, message, clients);}

  protected final Collection<ClientDescriptor> getClients() {return entityInvoker.getClients();}

  protected final S getSynchronizer() {
    return synchronizer;
  }

  protected final M getMessenger() {
    return messenger;
  }

  final ProxyInvoker<?> getEntityInvoker() {
    return entityInvoker;
  }

  final void setSynchronizer(S synchronizer) {
    this.synchronizer = synchronizer;
  }

  final void setMessenger(M messenger) {
    this.messenger = messenger;
  }

  final void setReconnect(Class<R> reconnectDataType, Codec codec) {
    this.reconnectDataType = reconnectDataType;
    this.codec = codec;
  }
}
