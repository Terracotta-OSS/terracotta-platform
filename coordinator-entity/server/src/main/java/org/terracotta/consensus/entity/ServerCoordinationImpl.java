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

import org.terracotta.consensus.entity.messages.ServerElectionEvent;
import org.terracotta.consensus.entity.server.LeaderElector;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.voltron.proxy.ClientId;
import org.terracotta.voltron.proxy.server.messages.MessageFiring;

/**
 * @author Alex Snaps
 */
class ServerCoordinationImpl extends MessageFiring implements CoordinationEntity {
  private final LeaderElector<String, ClientDescriptor> leaderElector;

  public ServerCoordinationImpl(final LeaderElector<String, ClientDescriptor> leaderElector, Class... messageTypes) {
    super(messageTypes);
    this.leaderElector = leaderElector;
  }

  @Override
  public ElectionResponse runForElection(final String namespace, @ClientId final Object clientId) {
    return leaderElector.enlist(namespace, (ClientDescriptor)clientId);
  }

  @Override
  public void accept(final String namespace, final LeaderOffer permit) {
    leaderElector.accept(namespace, permit);
    fire(ServerElectionEvent.completed(namespace));
  }

  @Override
  public void delist(final String namespace, @ClientId final Object clientId) {
    leaderElector.delist(namespace, (ClientDescriptor)clientId);
  }

}
