/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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
package org.terracotta.dynamic_config.entity.topology.client;

import org.terracotta.dynamic_config.entity.topology.common.Codec;
import org.terracotta.dynamic_config.entity.topology.common.Message;
import org.terracotta.dynamic_config.entity.topology.common.Response;
import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.entity.EntityClientService;

/**
 * @author Mathieu Carbou
 */
public class DynamicTopologyEntityClientService implements EntityClientService<DynamicTopologyEntity, Void, Message, Response, DynamicTopologyEntity.Settings> {

  private final Codec messageCodec = new Codec();

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
  public DynamicTopologyEntity create(EntityClientEndpoint<Message, Response> endpoint, DynamicTopologyEntity.Settings settings) {
    return new DynamicTopologyEntityImpl(endpoint, settings);
  }

  @Override
  public Codec getMessageCodec() {
    return messageCodec;
  }
}
