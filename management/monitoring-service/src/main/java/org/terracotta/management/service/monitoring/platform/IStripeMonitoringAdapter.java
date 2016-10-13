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
package org.terracotta.management.service.monitoring.platform;

import org.terracotta.monitoring.PlatformClientFetchedEntity;
import org.terracotta.monitoring.PlatformConnectedClient;
import org.terracotta.monitoring.PlatformEntity;
import org.terracotta.monitoring.PlatformServer;
import org.terracotta.monitoring.ServerState;

import java.io.Serializable;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.util.Objects.requireNonNull;

/**
 * Adapts the API-wanted {@link IStripeMonitoring} into the current existing one ({@link org.terracotta.monitoring.IStripeMonitoring}),
 * that is still currently using addNode / removeNode methods linked to a tree structure
 * <p>
 * This class's goal is to be removed when the wanted methods will be in tc-api.
 *
 * @author Mathieu Carbou
 */
public final class IStripeMonitoringAdapter implements org.terracotta.monitoring.IStripeMonitoring {

  private final IStripeMonitoring delegate;
  private final ConcurrentMap<PlatformServer, ConcurrentMap<String, PlatformEntity>> entities = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, PlatformConnectedClient> clients = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, PlatformClientFetchedEntity> fetches = new ConcurrentHashMap<>();

  private volatile PlatformServer currentActive;

  public IStripeMonitoringAdapter(IStripeMonitoring delegate) {
    this.delegate = delegate;
  }

  @Override
  public void serverDidBecomeActive(PlatformServer self) {
    this.currentActive = self;
    entities.put(currentActive, new ConcurrentHashMap<>());
    delegate.serverDidBecomeActive(self);
  }

  @Override
  public void serverDidJoinStripe(PlatformServer server) {
    entities.put(server, new ConcurrentHashMap<>());
    delegate.serverDidJoinStripe(server);
  }

  @Override
  public void serverDidLeaveStripe(PlatformServer server) {
    entities.remove(server);
    delegate.serverDidLeaveStripe(server);
  }

  @Override
  public void pushBestEffortsData(PlatformServer sender, String name, Serializable data) {
    delegate.pushBestEffortsData(sender, name, data);
  }

  @Override
  @Deprecated
  public boolean addNode(PlatformServer sender, String[] parents, String name, Serializable value) {
    if (parents == null || parents.length == 0) {
      return true;
    }

    if ("platform".equals(parents[0])) {
      // handle platform/state compared to platform/[clients|entities|fetched]/<id>
      String entryType = "state".equals(name) ? name : parents[parents.length - 1];

      switch (entryType) {

        case "entities": {
          requireNonNull(entities.get(sender), "Inconsistent monitoring tree: server did not joined stripe first: " + sender)
              .put(name, (PlatformEntity) value);
          delegate.serverEntityCreated(sender, (PlatformEntity) value);
          return true;
        }

        case "clients": {
          clients.put(name, (PlatformConnectedClient) value);
          delegate.clientConnected((PlatformConnectedClient) value);
          return true;
        }

        case "state": {
          delegate.serverStateChanged(sender, (ServerState) value);
          return true;
        }

        case "fetched": {
          PlatformClientFetchedEntity fetch = (PlatformClientFetchedEntity) value;
          PlatformConnectedClient client = clients.get(fetch.clientIdentifier);
          if (client == null) {
            throw new IllegalStateException("No " + PlatformConnectedClient.class.getSimpleName() + " has been added before with identifier " + fetch.clientIdentifier);
          }
          PlatformEntity entity = requireNonNull(entities.get(sender), "Inconsistent monitoring tree: server did not joined stripe first: " + sender)
              .get(fetch.entityIdentifier);
          requireNonNull(entity, "Inconsistent monitoring tree: entity " + fetch.entityIdentifier + " is not on server " + sender);
          fetches.put(name, (PlatformClientFetchedEntity) value);
          delegate.clientFetch(client, entity, fetch.clientDescriptor);
          return true;
        }

        default: {
          if (value != null) {
            // oups, we miss something ?
            throw new UnsupportedOperationException("addNode(" + String.join("/", (CharSequence[]) parents) + "/" + name + ") from server " + sender + ", data: " + value);

          } else {
            // calls to create the tree structure
            return true;
          }
        }

      }

    } else {
      String[] path = Arrays.copyOf(parents, parents.length + 1);
      path[parents.length] = name;
      delegate.setState(sender, path, value);
      return true;
    }
  }

  @Override
  @Deprecated
  public boolean removeNode(PlatformServer sender, String[] parents, String name) {
    if (parents == null || parents.length == 0) {
      return true;
    }
    switch (parents[parents.length - 1]) {

      case "entities": {
        PlatformEntity entity = requireNonNull(entities.get(sender), "Inconsistent monitoring tree: server did not joined stripe first: " + sender)
            .remove(name);
        requireNonNull(entity, "Inconsistent monitoring tree: entity " + name + " not found on server " + sender);
        delegate.serverEntityDestroyed(sender, entity);
        return true;
      }

      case "clients": {
        PlatformConnectedClient client = clients.remove(name);
        requireNonNull(client, "Inconsistent monitoring tree: client " + name + " not found on server " + sender);
        delegate.clientDisconnected(client);
        return true;
      }

      case "fetched": {
        PlatformClientFetchedEntity fetch = fetches.remove(name);
        requireNonNull(fetch, "Inconsistent monitoring tree: fetch " + name + " not found on server " + sender);

        PlatformEntity entity = requireNonNull(entities.get(sender), "Inconsistent monitoring tree: server did not joined stripe first: " + sender)
            .get(fetch.entityIdentifier);
        requireNonNull(entity, "Inconsistent monitoring tree: entity " + fetch.entityIdentifier + " not found on server " + sender);

        PlatformConnectedClient client = clients.get(fetch.clientIdentifier);
        requireNonNull(client, "Inconsistent monitoring tree: client " + fetch.clientIdentifier + " not found on server " + sender);

        delegate.clientUnfetch(client, entity, fetch.clientDescriptor);
        return true;
      }

      default: {
        return true;
      }

    }
  }

}
