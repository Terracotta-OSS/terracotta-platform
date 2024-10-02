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
package org.terracotta.management.service.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.monitoring.IStripeMonitoring;
import org.terracotta.monitoring.PlatformClientFetchedEntity;
import org.terracotta.monitoring.PlatformConnectedClient;
import org.terracotta.monitoring.PlatformEntity;
import org.terracotta.monitoring.PlatformServer;
import org.terracotta.monitoring.ServerState;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Adapts the API-wanted {@link PlatformListener} into the current existing ({@link org.terracotta.monitoring.IStripeMonitoring}),
 * that is still currently using addNode / removeNode methods linked to a tree structure
 * <p>
 * This class's goal is to receive only platform events (consumer id 0)
 *
 * @author Mathieu Carbou
 */
final class IStripeMonitoringPlatformListenerAdapter implements IStripeMonitoring {

  private static final Logger LOGGER = LoggerFactory.getLogger(IStripeMonitoringPlatformListenerAdapter.class);

  private final PlatformListener delegate;
  private final ConcurrentMap<String, Map<String, PlatformEntity>> entities = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, PlatformConnectedClient> clients = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, PlatformClientFetchedEntity> fetches = new ConcurrentHashMap<>();

  private volatile PlatformServer currentActive;

  IStripeMonitoringPlatformListenerAdapter(PlatformListener delegate) {
    this.delegate = Objects.requireNonNull(delegate);
  }

  @Override
  public void serverDidBecomeActive(PlatformServer self) {
    LOGGER.trace("[0] serverDidBecomeActive({})", self.getServerName());
    entities.put(self.getServerName(), new ConcurrentHashMap<>());
    currentActive = self;
    delegate.serverDidBecomeActive(self);
  }

  @Override
  public void serverDidJoinStripe(PlatformServer server) {
    LOGGER.trace("[0] serverDidJoinStripe({})", server.getServerName());
    entities.put(server.getServerName(), new ConcurrentHashMap<>());
    delegate.serverDidJoinStripe(server);
  }

  @Override
  public void serverDidLeaveStripe(PlatformServer server) {
    LOGGER.trace("[0] serverDidLeaveStripe({})", server.getServerName());
    entities.remove(server.getServerName());
    delegate.serverDidLeaveStripe(server);
  }

  @Override
  public boolean addNode(PlatformServer sender, String[] parents, String name, Serializable value) {
    if (parents == null) {
      return true;
    }
    if (parents.length < 2 && !name.equals("state")) {
      return true;
    }
    LOGGER.trace("[0] addNode({}, {}, {})", sender.getServerName(), String.join("/", parents), name);

    if ("platform".equals(parents[0])) {
      // handle platform/state compared to platform/[clients|entities|fetched]/<id>
      String entryType = "state".equals(name) ? name : parents[1];

      switch (entryType) {

        case "entities": {
          PlatformEntity platformEntity = (PlatformEntity) value;
          Map<String, PlatformEntity> serverEntities = entities.get(sender.getServerName());
          if (serverEntities == null) {
            Utils.warnOrAssert(LOGGER, "[0] addNode({}, {}, {}): unable to add entity: server did not joined stripe first", sender.getServerName(), String.join("/", parents), name);
            return false;
          }
          PlatformEntity previous = serverEntities.put(name, platformEntity);
          if (platformEntity.isActive || !sender.getServerName().equals(currentActive.getServerName())) {
            if (previous == null) {
              delegate.serverEntityCreated(sender, platformEntity);
            } else {
              delegate.serverEntityReconfigured(sender, platformEntity);
            }
          }
          return true;
        }

        case "clients": {
          if (currentActive == null) {
            Utils.warnOrAssert(LOGGER, "[0] addNode({}, {}, {}): unable to add client: not an active server", sender.getServerName(), String.join("/", parents), name);
            return false;
          }
          if (value instanceof PlatformConnectedClient) {
            clients.put(name, (PlatformConnectedClient) value);
            delegate.clientConnected(currentActive, (PlatformConnectedClient) value);
          } else {
            delegate.clientAddProperty(clients.get(parents[parents.length-1]), name, value.toString());
          }
          return true;
        }

        case "state": {
          delegate.serverStateChanged(sender, (ServerState) value);
          return false; // false to avoid replay of the cached data when a passive server becomes active
        }

        case "fetched": {
          PlatformClientFetchedEntity fetch = (PlatformClientFetchedEntity) value;
          Map<String, PlatformEntity> serverEntities = entities.get(sender.getServerName());
          if (serverEntities == null) {
            Utils.warnOrAssert(LOGGER, "[0] addNode({}, {}, {}): unable to add fetch for entity {} from client {}: server did not joined stripe first", sender.getServerName(), String.join("/", parents), name, fetch.entityIdentifier, fetch.clientIdentifier);
            return false;
          }
          PlatformEntity entity = serverEntities.get(fetch.entityIdentifier);
          if (entity == null) {
            Utils.warnOrAssert(LOGGER, "[0] addNode({}, {}, {}): unable to add fetch for entity {} from client {}: entity not found", sender.getServerName(), String.join("/", parents), name, fetch.entityIdentifier, fetch.clientIdentifier);
            return false;
          }
          PlatformConnectedClient client = clients.get(fetch.clientIdentifier);
          if (client == null) {
            Utils.warnOrAssert(LOGGER, "[0] addNode({}, {}, {}): unable to add fetch for entity {} from client {}: client not found", sender.getServerName(), String.join("/", parents), name, fetch.entityIdentifier, fetch.clientIdentifier);
            return false;
          }
          fetches.put(name, (PlatformClientFetchedEntity) value);
          delegate.clientFetch(client, entity, fetch.clientDescriptor);
          return true;
        }

        default: {
          if (value == null) {
            return true;
          }
        }

      }

    }

    throw new UnsupportedOperationException("addNode(" + String.join("/", (CharSequence[]) parents) + "/" + name + ") from server " + sender.getServerName() + ", data: " + value);
  }

  @Override
  public boolean removeNode(PlatformServer sender, String[] parents, String name) {
    if (parents == null || parents.length == 0) {
      return true;
    }
    LOGGER.trace("[0] removeNode({}, {}, {})", sender.getServerName(), String.join("/", parents), name);

    switch (parents[parents.length - 1]) {

      case "entities": {
        Map<String, PlatformEntity> serverEntities = entities.get(sender.getServerName());
        PlatformEntity entity = serverEntities.remove(name);
        if (entity != null) {
          delegate.serverEntityDestroyed(sender, entity);
          return true;
        }
        return false;
      }

      case "clients": {
        if (currentActive == null) {
          Utils.warnOrAssert(LOGGER, "[0] removeNode({}, {}, {}): unable to remove client: not an active server", sender.getServerName(), String.join("/", parents), name);
          return false;
        }
        PlatformConnectedClient client = clients.remove(name);
        if (client != null) {
          delegate.clientDisconnected(currentActive, client);
          return true;
        } else {
          Utils.warnOrAssert(LOGGER, "[0] removeNode({}, {}, {}): unable to remove client: not in current topology", sender.getServerName(), String.join("/", parents), name);
          return false;
        }
      }

      case "fetched": {
        PlatformClientFetchedEntity fetch = fetches.remove(name);
        if (fetch != null) {
          Map<String, PlatformEntity> serverEntities = entities.get(sender.getServerName());
          if (serverEntities == null) {
            Utils.warnOrAssert(LOGGER, "[0] removeNode({}, {}, {}): unable to remove fetch for entity {} from client {}: server did not joined stripe first", sender.getServerName(), String.join("/", parents), name, fetch.entityIdentifier, fetch.clientIdentifier);
            return false;
          }
          PlatformEntity entity = serverEntities.get(fetch.entityIdentifier);
          if (entity == null) {
            Utils.warnOrAssert(LOGGER, "[0] removeNode({}, {}, {}): unable to remove fetch for entity {} from client {}: entity not found", sender.getServerName(), String.join("/", parents), name, fetch.entityIdentifier, fetch.clientIdentifier);
            return false;
          }
          PlatformConnectedClient client = clients.get(fetch.clientIdentifier);
          if (client == null) {
            Utils.warnOrAssert(LOGGER, "[0] removeNode({}, {}, {}): unable to remove fetch for entity {} from client {}: client not found", sender.getServerName(), String.join("/", parents), name, fetch.entityIdentifier, fetch.clientIdentifier);
            return false;
          }
          delegate.clientUnfetch(client, entity, fetch.clientDescriptor);
          return true;
        } else {
          Utils.warnOrAssert(LOGGER, "[0] removeNode({}, {}, {}): unable to remove fetch: not in current topology", sender.getServerName(), String.join("/", parents), name);
          return false;
        }
      }

      default: {
        return true;
      }

    }
  }

  @Override
  public void pushBestEffortsData(PlatformServer sender, String name, Serializable data) {
    throw new UnsupportedOperationException();
  }

}
