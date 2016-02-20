/*
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Connection API.
 *
 * The Initial Developer of the Covered Software is
 * Terracotta, Inc., a Software AG company
 */

package org.terracotta.voltron.proxy.server;

import org.terracotta.entity.ActiveServerEntity;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.PassiveSynchronizationChannel;
import org.terracotta.voltron.proxy.server.messages.ProxyEntityMessage;
import org.terracotta.voltron.proxy.server.messages.ProxyEntityResponse;

/**
 * @author Alex Snaps
 */
public abstract class ProxiedServerEntity<T> implements ActiveServerEntity<ProxyEntityMessage, ProxyEntityResponse> {

  private final ProxyInvoker<T> target;

  public ProxiedServerEntity(final ProxyInvoker<T> target) {
    this.target = target;
  }

  public ProxyEntityResponse invoke(final ClientDescriptor clientDescriptor, final ProxyEntityMessage msg) {
    return target.invoke(clientDescriptor, msg);
  }

  public void connected(ClientDescriptor clientDescriptor) {
    target.addClient(clientDescriptor);
  }

  public void disconnected(ClientDescriptor clientDescriptor) {
    target.removeClient(clientDescriptor);
  }

  public byte[] getConfig() {
    return null;
  }

  public void handleReconnect(final ClientDescriptor clientDescriptor, final byte[] bytes) {
    // Don't care I think
  }

  public void synchronizeKeyToPassive(final PassiveSynchronizationChannel passiveSynchronizationChannel, final int i) {
    // no op ... for now?
  }

  public void createNew() {
    // Don't care I think
  }

  public void loadExisting() {
    // Don't care I think
  }

  public void destroy() {
    // Don't care I think
  }

  protected void fireAndForgetMessage(Object message, ClientDescriptor ... clients) {
    target.fireAndForgetMessage(message, clients);
  }
}
