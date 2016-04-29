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
import org.terracotta.entity.BasicServiceConfiguration;
import org.terracotta.entity.ClientCommunicator;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.ServiceRegistry;
import org.terracotta.voltron.proxy.SerializationCodec;
import org.terracotta.voltron.proxy.server.ProxyServerEntityService;

/**
 * @author Alex Snaps
 */
public class CoordinationServerEntityService extends ProxyServerEntityService<Void> {
  
  private static final String ENTITY_CLASS_NAME = "org.terracotta.consensus.entity.client.CoordinationClientEntity";

  public CoordinationServerEntityService() {
    super(CoordinationEntity.class, Void.TYPE, new SerializationCodec(), ServerElectionEvent.class);
  }

  @Override
  public long getVersion() {
    return Versions.LATEST.version();
  }

  @Override
  public boolean handlesEntityType(final String s) {
    return ENTITY_CLASS_NAME.equals(s);
  }

  @Override
  public CoordinationServerEntity createActiveEntity(final ServiceRegistry serviceRegistry, Void v) {
    ClientCommunicator communicator = serviceRegistry.getService(new BasicServiceConfiguration<ClientCommunicator>(ClientCommunicator.class));
    
    return new CoordinationServerEntity(new LeaderElector<String, ClientDescriptor>(new UuidOfferFactory()), communicator);
  }

}
