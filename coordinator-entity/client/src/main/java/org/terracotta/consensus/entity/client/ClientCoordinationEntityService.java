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

package org.terracotta.consensus.entity.client;

import org.terracotta.voltron.proxy.client.ProxyEntityClientService;
import org.terracotta.consensus.entity.CoordinationEntity;
import org.terracotta.consensus.entity.messages.ServerElectionEvent;

/**
 * @author Alex Snaps
 */
public class ClientCoordinationEntityService extends ProxyEntityClientService<CoordinationClientEntity, Void> {

  public ClientCoordinationEntityService() {
    super(CoordinationClientEntity.class, CoordinationEntity.class, Void.TYPE, ServerElectionEvent.class);
  }

}
