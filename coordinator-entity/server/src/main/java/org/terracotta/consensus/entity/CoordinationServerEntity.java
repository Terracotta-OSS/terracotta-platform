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

package org.terracotta.consensus.entity;

import org.terracotta.consensus.entity.messages.LeaderElected;
import org.terracotta.consensus.entity.server.DelistListener;
import org.terracotta.consensus.entity.server.LeaderElector;
import org.terracotta.entity.ClientCommunicator;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.voltron.proxy.SerializationCodec;
import org.terracotta.voltron.proxy.server.ProxiedServerEntity;
import org.terracotta.voltron.proxy.server.ProxyInvoker;

/**
 * @author Alex Snaps
 */
public class CoordinationServerEntity extends ProxiedServerEntity {

  private final LeaderElector<String, ClientDescriptor> leaderElector;

  public CoordinationServerEntity(final LeaderElector<String, ClientDescriptor> leaderElector, final ClientCommunicator clientCommunicator) {
    super(new ProxyInvoker(CoordinationEntity.class, new ServerCoordinationImpl(leaderElector, LeaderElected.class), new SerializationCodec(), LeaderElected.class));
    this.leaderElector = leaderElector;
    this.target.setClientCommunicator(clientCommunicator);
    this.leaderElector.setListener(new DelistListenerImpl<String>(target, clientCommunicator));
  }

  @Override
  public void connected(final ClientDescriptor clientDescriptor) {
    target.addClient(clientDescriptor);
  }

  @Override
  public void disconnected(final ClientDescriptor clientDescriptor) {
    target.removeClient(clientDescriptor);
    leaderElector.delistAll(clientDescriptor);
  }

  private static class DelistListenerImpl<K> implements DelistListener<K, ClientDescriptor> {
    
    private final ProxyInvoker target; 
    private final ClientCommunicator communicator; 
    
    public DelistListenerImpl(ProxyInvoker target, ClientCommunicator communicator) {
      this.target = target;
      this.communicator = communicator;
    }

    public void onDelist(K key, ClientDescriptor clientDescriptor, Nomination permit) {
      target.fireAndForgetMessage(communicator, permit, clientDescriptor);
    }
  }

}
