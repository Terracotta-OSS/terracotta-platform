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
import org.terracotta.entity.ActiveServerEntity;
import org.terracotta.entity.ClientCommunicator;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.ConcurrencyStrategy;
import org.terracotta.entity.NoConcurrencyStrategy;
import org.terracotta.voltron.proxy.SerializationCodec;
import org.terracotta.voltron.proxy.server.ProxyInvoker;

/**
 * @author Alex Snaps
 */
public class CoordinationServerEntity implements ActiveServerEntity {

  private final LeaderElector<String, ClientDescriptor> leaderElector;
  private final ProxyInvoker target;

  public CoordinationServerEntity(final LeaderElector<String, ClientDescriptor> leaderElector, final ClientCommunicator clientCommunicator) {
    this.leaderElector = leaderElector;
    this.target = new ProxyInvoker(CoordinationEntity.class, new ServerCoordinationImpl(this.leaderElector, LeaderElected.class), new SerializationCodec(), LeaderElected.class);
    this.target.setClientCommunicator(clientCommunicator);
    this.leaderElector.setListener(new DelistListenerImpl<String>(target, clientCommunicator));
  }

  public byte[] invoke(final ClientDescriptor clientDescriptor, final byte[] arg) {
    return target.invoke(clientDescriptor, arg);
  }

  public ConcurrencyStrategy getConcurrencyStrategy() {
    //TODO: We can implment a PerKeyConcurrencyStrategy
    return new NoConcurrencyStrategy();
  }

  public void connected(final ClientDescriptor clientDescriptor) {
    target.addClient(clientDescriptor);
  }

  public void disconnected(final ClientDescriptor clientDescriptor) {
    target.removeClient(clientDescriptor);
    leaderElector.delistAll(clientDescriptor);
  }

  public byte[] getConfig() {
    return null;
  }

  public void handleReconnect(final ClientDescriptor clientDescriptor, final byte[] bytes) {
    // Don't care I think
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
