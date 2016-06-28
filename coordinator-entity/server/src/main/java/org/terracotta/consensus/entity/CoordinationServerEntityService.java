/**
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
