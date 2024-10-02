/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
package org.terracotta.nomad.entity.client;

import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.entity.EntityClientService;
import org.terracotta.nomad.entity.common.NomadEntityMessage;
import org.terracotta.nomad.entity.common.NomadEntityResponse;
import org.terracotta.nomad.entity.common.NomadMessageCodec;

/**
 * @author Mathieu Carbou
 */
public class NomadEntityClientService<T> implements EntityClientService<NomadEntity<T>, Void, NomadEntityMessage, NomadEntityResponse, NomadEntity.Settings> {

  private final NomadMessageCodec messageCodec = new NomadMessageCodec();

  @Override
  public boolean handlesEntityType(Class<NomadEntity<T>> cls) {
    return NomadEntity.class.equals(cls);
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
  public NomadEntity<T> create(EntityClientEndpoint<NomadEntityMessage, NomadEntityResponse> endpoint, NomadEntity.Settings settings) {
    return new NomadEntityImpl<>(endpoint, settings);
  }

  @Override
  public NomadMessageCodec getMessageCodec() {
    return messageCodec;
  }
}
