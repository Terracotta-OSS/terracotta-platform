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

import org.terracotta.entity.PassiveServerEntity;
import org.terracotta.voltron.proxy.ProxyEntityMessage;
import org.terracotta.voltron.proxy.ProxyEntityResponse;

import java.util.Objects;

/**
 * @author Mathieu Carbou
 */
public abstract class PassiveProxiedServerEntity<T, S, M> implements PassiveServerEntity<ProxyEntityMessage, ProxyEntityResponse> {

  private final T entity;
  private final S synchronizer;
  private final M messenger;

  private final ProxyInvoker<T> entityInvoker;
  private final ProxyInvoker<S> synchronizerInvoker;
  private final ProxyInvoker<M> messengerInvoker;

  public PassiveProxiedServerEntity(T entity, S synchronizer, M messenger) {
    this.entity = Objects.requireNonNull(entity);
    this.synchronizer = synchronizer; // can be null
    this.messenger = messenger; // can be null
    this.entityInvoker = new ProxyInvoker<>(entity);
    this.synchronizerInvoker = new ProxyInvoker<>(synchronizer);
    this.messengerInvoker = new ProxyInvoker<>(messenger);
  }

  @Override
  public void invoke(final ProxyEntityMessage msg) {
    switch (msg.getType()) {
      case SYNC:
        synchronizerInvoker.invoke(msg);
        break;
      case MESSENGER:
        messengerInvoker.invoke(msg);
        break;
      case MESSAGE:
        entityInvoker.invoke(msg);
        break;
      default:
        throw new AssertionError(msg.getType());
    }
  }

  @Override
  public void startSyncEntity() {

  }

  @Override
  public void endSyncEntity() {

  }

  @Override
  public void startSyncConcurrencyKey(int concurrencyKey) {

  }

  @Override
  public void endSyncConcurrencyKey(int concurrencyKey) {

  }

  @Override
  public void createNew() {
    // Don't care I think
  }

  @Override
  public void destroy() {
    // Don't care I think
  }

  protected final T getEntity() {
    return entity;
  }

  protected final S getSynchronizer() {
    return synchronizer;
  }

  protected final M getMessenger() {
    return messenger;
  }

}
