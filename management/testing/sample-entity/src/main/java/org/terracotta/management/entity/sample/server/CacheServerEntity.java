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
package org.terracotta.management.entity.sample.server;

import org.terracotta.entity.BasicServiceConfiguration;
import org.terracotta.entity.ClientCommunicator;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.MessageCodecException;
import org.terracotta.entity.ServiceRegistry;
import org.terracotta.management.entity.sample.Cache;
import org.terracotta.management.entity.sample.server.management.Management;
import org.terracotta.voltron.proxy.ProxyEntityResponse;
import org.terracotta.voltron.proxy.server.ProxiedServerEntity;

import java.io.Serializable;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author Mathieu Carbou
 */
class CacheServerEntity extends ProxiedServerEntity<Cache> {

  private final Queue<ClientDescriptor> clients = new ConcurrentLinkedQueue<>();
  private final Management management;
  private final ServerCache cache;

  CacheServerEntity(ServerCache cache, ServiceRegistry serviceRegistry) {
    super(cache);
    this.cache = cache;

    ClientCommunicator clientCommunicator = Objects.requireNonNull(serviceRegistry.getService(new BasicServiceConfiguration<>(ClientCommunicator.class)));

    // callback clients on eviction
    cache.setEvictionListener((key, value) -> {
      for (ClientDescriptor client : clients) {
        try {
          clientCommunicator.sendNoResponse(client, ProxyEntityResponse.response(Serializable[].class, new Serializable[]{"eviction", key, value}));
        } catch (MessageCodecException e) {
          e.printStackTrace(); // ok: it is a sample test entity - we do not care
        }
      }
    });

    this.management = new Management(serviceRegistry);
  }

  @Override
  public void connected(ClientDescriptor clientDescriptor) {
    super.connected(clientDescriptor);
    clients.offer(clientDescriptor);
  }

  @Override
  public void disconnected(ClientDescriptor clientDescriptor) {
    clients.remove(clientDescriptor);
    super.disconnected(clientDescriptor);
  }

  @Override
  public void createNew() {
    super.createNew();
    management.init();
    management.serverCacheCreated(cache);
  }

  @Override
  public void destroy() {
    management.serverCacheDestroyed(cache);
    management.close();
    super.destroy();
  }
}
