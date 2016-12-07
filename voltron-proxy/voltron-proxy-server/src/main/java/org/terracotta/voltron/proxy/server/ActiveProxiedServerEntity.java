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

import org.terracotta.entity.ActiveServerEntity;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.PassiveSynchronizationChannel;
import org.terracotta.voltron.proxy.ProxyEntityMessage;
import org.terracotta.voltron.proxy.ProxyEntityResponse;

import java.util.Objects;

/**
 * @author Alex Snaps
 */
public abstract class ActiveProxiedServerEntity<T, S> implements ActiveServerEntity<ProxyEntityMessage, ProxyEntityResponse> {

  private final T entity;
  private final ProxyInvoker<T> invoker;
  private S synchronizer;

  public ActiveProxiedServerEntity(T entity) {
    this.entity = Objects.requireNonNull(entity);
    this.invoker = new ProxyInvoker<>(entity);
  }

  @Override
  public ProxyEntityResponse invoke(final ClientDescriptor clientDescriptor, final ProxyEntityMessage msg) {
    return invoker.invoke(msg, clientDescriptor);
  }

  @Override
  public void connected(ClientDescriptor clientDescriptor) {
    invoker.addClient(clientDescriptor);
  }

  @Override
  public void disconnected(ClientDescriptor clientDescriptor) {
    invoker.removeClient(clientDescriptor);
  }

  @Override
  public void handleReconnect(final ClientDescriptor clientDescriptor, final byte[] extendedReconnectData) {
    // Don't care I think
  }

  @Override
  public void synchronizeKeyToPassive(final PassiveSynchronizationChannel<ProxyEntityMessage> channel, final int concurrencyKey) {
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
    // Don't care I think
  }

  protected void synchronizeKeyToPassive(int concurrencyKey) {
  }

  protected <M> void fireMessage(Class<M> type, M message, boolean echo) {invoker.fireMessage(type, message, echo);}

  protected <M> void fireMessage(Class<M> type, M message, ClientDescriptor[] clients) {invoker.fireMessage(type, message, clients);}

  protected T getEntity() {
    return entity;
  }

  protected S getSynchronizer() {
    return synchronizer;
  }

  ProxyInvoker<T> getInvoker() {
    return invoker;
  }

  void setSynchronizer(S synchronizer) {
    this.synchronizer = synchronizer;
  }

}
