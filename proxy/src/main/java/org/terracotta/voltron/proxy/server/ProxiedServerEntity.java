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
import org.terracotta.entity.ClientCommunicator;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.PassiveSynchronizationChannel;
import org.terracotta.voltron.proxy.server.messages.ProxyEntityMessage;
import org.terracotta.voltron.proxy.server.messages.ProxyEntityResponse;

/**
 * @author Alex Snaps
 */
public abstract class ProxiedServerEntity<T> implements ActiveServerEntity<ProxyEntityMessage, ProxyEntityResponse> {

  private final ProxyInvoker<T> target;

  public ProxiedServerEntity(T target) {
    this(target, null);
  }

  public ProxiedServerEntity(T target, ClientCommunicator clientCommunicator, Class<?> ... messageTypes) {
    this.target = new ProxyInvoker<T>(target, clientCommunicator, messageTypes);
  }

  public T getTarget() {
    return target.getTarget();
  }

  @Override
  public ProxyEntityResponse invoke(final ClientDescriptor clientDescriptor, final ProxyEntityMessage msg) {
    return target.invoke(clientDescriptor, msg);
  }

  @Override
  public void connected(ClientDescriptor clientDescriptor) {
    target.addClient(clientDescriptor);
  }

  @Override
  public void disconnected(ClientDescriptor clientDescriptor) {
    target.removeClient(clientDescriptor);
  }

  @Override
  public void handleReconnect(final ClientDescriptor clientDescriptor, final byte[] bytes) {
    // Don't care I think
  }

  @Override
  public void synchronizeKeyToPassive(final PassiveSynchronizationChannel passiveSynchronizationChannel, final int i) {
    // no op ... for now?
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

  protected void fireAndForgetMessage(Object message, ClientDescriptor ... clients) {
    target.fireAndForgetMessage(message, clients);
  }
}
