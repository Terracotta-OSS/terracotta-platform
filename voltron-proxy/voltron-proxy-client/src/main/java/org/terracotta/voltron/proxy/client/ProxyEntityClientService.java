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
package org.terracotta.voltron.proxy.client;

import org.terracotta.connection.entity.Entity;
import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.entity.EntityClientService;
import org.terracotta.entity.MessageCodec;
import org.terracotta.voltron.proxy.Codec;
import org.terracotta.voltron.proxy.ProxyEntityMessage;
import org.terracotta.voltron.proxy.ProxyEntityResponse;
import org.terracotta.voltron.proxy.ProxyMessageCodec;

public abstract class ProxyEntityClientService<T extends Entity & ServerMessageAware, C> implements EntityClientService<T, C, ProxyEntityMessage, ProxyEntityResponse, Object> {

  private final Class<T> clientType;
  private final Class<? super T> type;
  private final Class<C> configType;
  private final Class<?>[] messageTypes;
  private final ProxyMessageCodec messageCodec;

  public ProxyEntityClientService(Class<T> clientType, Class<? super T> type, Class<C> configType, Class<?>[] messageTypes) {
    this.clientType = clientType;
    this.type = type;
    this.configType = configType;
    this.messageTypes = messageTypes;
    this.messageCodec = new ProxyMessageCodec(type, messageTypes);
  }

  @Override
  public boolean handlesEntityType(final Class<T> aClass) {
    return aClass == clientType;
  }

  @Override
  public T create(EntityClientEndpoint<ProxyEntityMessage, ProxyEntityResponse> endpoint, Object userData) {
    return ClientProxyFactory.createEntityProxy(clientType, type, endpoint, messageTypes, messageCodec.getCodec());
  }

  @Override
  public MessageCodec<ProxyEntityMessage, ProxyEntityResponse> getMessageCodec() {
    return messageCodec;
  }

  @Override
  public C deserializeConfiguration(byte[] configuration) {
    if (configType == Void.TYPE) {
      return null;
    }
    return configType.cast(messageCodec.getCodec().decode(configType, configuration));
  }

  @Override
  public byte[] serializeConfiguration(C configuration) {
    if (configType == Void.TYPE) {
      return new byte[0];
    }
    return messageCodec.getCodec().encode(configType, configuration);
  }

  protected void setCodec(Codec codec) {
    messageCodec.setCodec(codec);
  }
}
