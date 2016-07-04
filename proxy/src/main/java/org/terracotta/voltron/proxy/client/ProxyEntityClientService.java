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
import org.terracotta.voltron.proxy.ProxyMessageCodec;
import org.terracotta.voltron.proxy.SerializationCodec;
import org.terracotta.voltron.proxy.server.messages.ProxyEntityMessage;
import org.terracotta.voltron.proxy.server.messages.ProxyEntityResponse;

public abstract class ProxyEntityClientService<T extends Entity, C> implements EntityClientService<T, C, ProxyEntityMessage, ProxyEntityResponse> {

  private final Class<T> clientType;
  private final Class<? super T> type;
  private final Codec codec;
  private final Class<?>[] messageTypes;
  private final Class<C> configType;

  public ProxyEntityClientService(Class<T> clientType, Class<? super T> type, Class<C> configType, Class<?>... messageTypes) {
    this(clientType, type, configType, new SerializationCodec(), messageTypes);
  }

  public ProxyEntityClientService(Class<T> clientType, Class<? super T> type, Class<C> configType) {
    this(clientType, type, configType, new SerializationCodec());
  }

  public ProxyEntityClientService(Class<T> clientType, Class<? super T> type, Class<C> configType, Codec codec, Class<?>... messageTypes) {
    this.clientType = clientType;
    this.type = type;
    this.configType = configType;
    this.codec = codec;
    this.messageTypes = messageTypes;
  }

  @Override
  public boolean handlesEntityType(final Class<T> aClass) {
    return aClass == clientType;
  }

  @Override
  public T create(EntityClientEndpoint<ProxyEntityMessage, ProxyEntityResponse> endpoint) {
    return (T) ClientProxyFactory.createEntityProxy((Class) clientType, type, endpoint, messageTypes);
  }

  @Override
  public MessageCodec<ProxyEntityMessage, ProxyEntityResponse> getMessageCodec() {
    return new ProxyMessageCodec(codec, type, messageTypes);
  }

  @Override
  public C deserializeConfiguration(byte[] configuration) {
    if(configType == Void.TYPE) {
      return null;
    }
    return configType.cast(codec.decode(configuration, configType));
  }

  @Override
  public byte[] serializeConfiguration(C configuration) {
    if(configType == Void.TYPE) {
      return new byte[0];
    }
    return codec.encode(configType, configuration);
  }

  private static Class<?>[] sum(Class<?> one, Class<?>[] others) {
    Class<?>[] result = new Class<?>[others.length + 1];
    result[0] = one;
    System.arraycopy(others, 0, result, 1, others.length);
    return result;
  }
}
