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
package org.terracotta.voltron.proxy.server;

import org.terracotta.entity.BasicServiceConfiguration;
import org.terracotta.entity.ClientCommunicator;
import org.terracotta.entity.ConcurrencyStrategy;
import org.terracotta.entity.EntityServerService;
import org.terracotta.entity.ExecutionStrategy;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.ServiceRegistry;
import org.terracotta.entity.SyncMessageCodec;
import org.terracotta.voltron.proxy.Codec;
import org.terracotta.voltron.proxy.ProxyEntityMessage;
import org.terracotta.voltron.proxy.ProxyEntityResponse;
import org.terracotta.voltron.proxy.ProxyMessageCodec;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * @author Mathieu Carbou
 */
public abstract class ProxyServerEntityService<T, C, S> implements EntityServerService<ProxyEntityMessage, ProxyEntityResponse> {

  private final Class<C> configType;
  private final Class<?>[] eventTypes;
  private final Class<S> synchronizerType;
  private final ProxyMessageCodec messageCodec;
  private final DelegatingSyncMessageCodec syncMessageCodec;
  private final ExecutionStrategy<ProxyEntityMessage> executionStrategy = message -> ExecutionStrategy.Location.valueOf(message.getExecutionLocation().name());
  private final ConcurrencyStrategy<ProxyEntityMessage> concurrencyStrategy = new ConcurrencyStrategy<ProxyEntityMessage>() {
    @Override
    public int concurrencyKey(ProxyEntityMessage message) {
      return message.getConcurrencyKey();
    }

    @Override
    public Set<Integer> getKeysForSynchronization() {
      return ProxyServerEntityService.this.getKeysForSynchronization();
    }
  };

  public ProxyServerEntityService(Class<T> proxyType, Class<C> configType, Class<?>[] eventTypes, Class<S> synchronizerType) {
    this.configType = Objects.requireNonNull(configType);

    this.eventTypes = eventTypes; // can be null
    this.messageCodec = new ProxyMessageCodec(Objects.requireNonNull(proxyType), eventTypes);

    this.synchronizerType = synchronizerType; // can be null
    this.syncMessageCodec = synchronizerType == null ? null : new DelegatingSyncMessageCodec(new ProxyMessageCodec(synchronizerType));
  }

  @Override
  public MessageCodec<ProxyEntityMessage, ProxyEntityResponse> getMessageCodec() {
    return messageCodec;
  }

  @Override
  public SyncMessageCodec<ProxyEntityMessage> getSyncMessageCodec() {
    return syncMessageCodec;
  }

  @Override
  public ActiveProxiedServerEntity<T, S> createActiveEntity(ServiceRegistry registry, byte[] configuration) {
    C config = null;
    if (configType == Void.TYPE) {
      if (configuration != null && configuration.length > 0) {
        throw new IllegalArgumentException("No config expected here!");
      }
    } else {
      config = configType.cast(messageCodec.getCodec().decode(configType, configuration));
    }
    ActiveProxiedServerEntity<T, S> activeEntity = createActiveEntity(registry, config);

    if (eventTypes != null && eventTypes.length > 0) {
      ClientCommunicator clientCommunicator = registry.getService(new BasicServiceConfiguration<>(ClientCommunicator.class));
      activeEntity.getInvoker().activateEvents(clientCommunicator, eventTypes);
    }

    if(synchronizerType != null) {
      S synchronizer = SyncProxyFactory.createProxy(synchronizerType);
      activeEntity.setSynchronizer(synchronizer);
    }

    return activeEntity;
  }

  @Override
  public PassiveProxiedServerEntity<T, S> createPassiveEntity(ServiceRegistry registry, byte[] configuration) {
    C config = null;
    if (configType == Void.TYPE) {
      if (configuration != null && configuration.length > 0) {
        throw new IllegalArgumentException("No config expected here!");
      }
    } else {
      config = configType.cast(messageCodec.getCodec().decode(configType, configuration));
    }
    return createPassiveEntity(registry, config);
  }

  @Override
  public ConcurrencyStrategy<ProxyEntityMessage> getConcurrencyStrategy(byte[] configuration) {
    return concurrencyStrategy;
  }

  @Override
  public ExecutionStrategy<ProxyEntityMessage> getExecutionStrategy(byte[] configuration) {
    return executionStrategy;
  }

  protected Set<Integer> getKeysForSynchronization() {
    return Collections.emptySet();
  }

  protected void setCodec(Codec codec) {
    messageCodec.setCodec(codec);
    if(syncMessageCodec != null) {
      syncMessageCodec.setCodec(codec);
    }
  }

  protected abstract ActiveProxiedServerEntity<T, S> createActiveEntity(ServiceRegistry registry, C configuration);

  protected abstract PassiveProxiedServerEntity<T, S> createPassiveEntity(ServiceRegistry registry, C configuration);

}
