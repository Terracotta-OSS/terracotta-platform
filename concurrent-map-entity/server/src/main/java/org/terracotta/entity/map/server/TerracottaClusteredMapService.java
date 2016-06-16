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
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is
 * Terracotta, Inc., a Software AG company
 */

package org.terracotta.entity.map.server;

import org.terracotta.entity.ActiveServerEntity;
import org.terracotta.entity.ConcurrencyStrategy;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.PassiveServerEntity;
import org.terracotta.entity.ServerEntityService;
import org.terracotta.entity.ServiceRegistry;
import org.terracotta.entity.SyncMessageCodec;
import org.terracotta.entity.map.common.ClusteredMapCodec;
import org.terracotta.entity.map.common.ConcurrentClusteredMap;
import org.terracotta.entity.map.common.MapOperation;
import org.terracotta.entity.map.common.MapResponse;

/**
 * TerracottaClusteredMapService
 */
public class TerracottaClusteredMapService implements ServerEntityService<MapOperation, MapResponse> {
  @Override
  public long getVersion() {
    return ConcurrentClusteredMap.VERSION;
  }

  @Override
  public boolean handlesEntityType(String typeName) {
    return typeName.equals(ConcurrentClusteredMap.class.getName());
  }

  @Override
  public ActiveServerEntity<MapOperation, MapResponse> createActiveEntity(ServiceRegistry registry, byte[] configuration) {
    return new ActiveTerracottaClusteredMap();
  }

  @Override
  public PassiveServerEntity<MapOperation, MapResponse> createPassiveEntity(ServiceRegistry registry, byte[] configuration) {
    return new PassiveTerracottaClusteredMap();
  }

  @Override
  public ConcurrencyStrategy<MapOperation> getConcurrencyStrategy(byte[] configuration) {
    return new ActiveTerracottaClusteredMap.MapConcurrencyStrategy();
  }

  @Override
  public MessageCodec<MapOperation, MapResponse> getMessageCodec() {
    return new ClusteredMapCodec();
  }

  @Override
  public SyncMessageCodec<MapOperation> getSyncMessageCodec() {
    return new ClusteredMapSyncCodec();
  }
}
