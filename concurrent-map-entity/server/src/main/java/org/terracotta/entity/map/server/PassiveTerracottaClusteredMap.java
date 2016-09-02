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
package org.terracotta.entity.map.server;

import org.terracotta.entity.PassiveServerEntity;
import org.terracotta.entity.ServiceRegistry;
import org.terracotta.entity.map.common.MapOperation;
import org.terracotta.entity.map.common.MapResponse;

import java.util.concurrent.ConcurrentMap;

/**
 * PassiveTerracottaClusteredMap
 */
class PassiveTerracottaClusteredMap extends AbstractClusteredMap implements PassiveServerEntity<MapOperation, MapResponse> {

  public PassiveTerracottaClusteredMap(ServiceRegistry services) {
    super(services);
  }

  @Override
  public void invoke(MapOperation operation) {
    switch (operation.operationType()) {
      case SYNC_OP:
        SyncOperation syncOperation = (SyncOperation) operation;
        ConcurrentMap<Object, Object> objectMap = syncOperation.getObjectMap();
        this.dataMap.putAll(objectMap);
        break;
      default:
        super.invokeInternal(operation);
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

}
