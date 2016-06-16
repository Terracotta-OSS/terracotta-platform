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

package org.terracotta.entity.map;

import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.entity.EntityClientService;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.map.common.ClusteredMapCodec;
import org.terracotta.entity.map.common.ConcurrentClusteredMap;
import org.terracotta.entity.map.common.MapOperation;
import org.terracotta.entity.map.common.MapResponse;

@SuppressWarnings("rawtypes")
public class TerracottaClusteredMapClientService implements EntityClientService<ConcurrentClusteredMap, Void, MapOperation, MapResponse> {
  @Override
  public boolean handlesEntityType(Class<ConcurrentClusteredMap> cls) {
    return cls == ConcurrentClusteredMap.class;
  }

  @Override
  public byte[] serializeConfiguration(Void configuration) {
    return new byte[0];
  }

  @Override
  public Void deserializeConfiguration(byte[] configuration) {
    return null;
  }

  @SuppressWarnings("unchecked")
  @Override
  public ConcurrentClusteredMap create(EntityClientEndpoint endpoint) {
    return new TerracottaClusteredMap(endpoint);
  }

  @Override
  public MessageCodec<MapOperation, MapResponse> getMessageCodec() {
    return new ClusteredMapCodec();
  }
}
