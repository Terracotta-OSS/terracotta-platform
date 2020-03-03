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
package org.terracotta.dynamic_config.entity.client;

import org.terracotta.dynamic_config.entity.common.DynamicTopologyEntityMessage;
import org.terracotta.dynamic_config.entity.common.DynamicTopologyMessageCodec;
import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.entity.EntityClientService;

/**
 * @author Mathieu Carbou
 */
public class DynamicTopologyEntityClientService implements EntityClientService<DynamicTopologyEntity, Void, DynamicTopologyEntityMessage, DynamicTopologyEntityMessage, DynamicTopologyEntity.Settings> {

  private final DynamicTopologyMessageCodec messageCodec = new DynamicTopologyMessageCodec();

  @Override
  public boolean handlesEntityType(Class<DynamicTopologyEntity> cls) {
    return DynamicTopologyEntity.class.equals(cls);
  }

  @Override
  public byte[] serializeConfiguration(Void configuration) {
    return new byte[0];
  }

  @Override
  public Void deserializeConfiguration(byte[] configuration) {
    return null;
  }

  @Override
  public DynamicTopologyEntity create(EntityClientEndpoint<DynamicTopologyEntityMessage, DynamicTopologyEntityMessage> endpoint, DynamicTopologyEntity.Settings settings) {
    return new DynamicTopologyEntityImpl(endpoint, settings);
  }

  @Override
  public DynamicTopologyMessageCodec getMessageCodec() {
    return messageCodec;
  }
}
