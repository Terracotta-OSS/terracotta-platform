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

import org.omg.CORBA.Object;
import org.terracotta.entity.PassiveServerEntity;
import org.terracotta.entity.map.common.MapOperation;
import org.terracotta.entity.map.common.MapResponse;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * PassiveTerracottaClusteredMap
 */
class PassiveTerracottaClusteredMap implements PassiveServerEntity<MapOperation, MapResponse> {

  private final ConcurrentMap<Object, Object> map = new ConcurrentHashMap<Object, Object>();

  @Override
  public void invoke(MapOperation message) {
    // do assign received map to instance map
    // or append content if message is an operation
  }

  @Override
  public void startSyncEntity() {
    throw new UnsupportedOperationException("TODO Implement me!");
  }

  @Override
  public void endSyncEntity() {
    throw new UnsupportedOperationException("TODO Implement me!");
  }

  @Override
  public void startSyncConcurrencyKey(int concurrencyKey) {
    throw new UnsupportedOperationException("TODO Implement me!");
  }

  @Override
  public void endSyncConcurrencyKey(int concurrencyKey) {
    throw new UnsupportedOperationException("TODO Implement me!");
  }

  @Override
  public void createNew() {
    throw new UnsupportedOperationException("TODO Implement me!");
  }

  @Override
  public void loadExisting() {
    throw new UnsupportedOperationException("TODO Implement me!");
  }

  @Override
  public void destroy() {
    throw new UnsupportedOperationException("TODO Implement me!");
  }
}
