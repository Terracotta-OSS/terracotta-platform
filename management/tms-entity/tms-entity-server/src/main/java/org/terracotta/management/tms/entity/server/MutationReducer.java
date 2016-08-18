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
package org.terracotta.management.tms.entity.server;

import org.terracotta.management.model.cluster.Client;
import org.terracotta.management.model.cluster.ClientIdentifier;
import org.terracotta.management.model.cluster.Cluster;
import org.terracotta.management.model.cluster.Server;
import org.terracotta.management.model.cluster.ServerEntity;
import org.terracotta.management.model.cluster.Stripe;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.service.monitoring.IMonitoringConsumer;
import org.terracotta.monitoring.PlatformClientFetchedEntity;
import org.terracotta.monitoring.PlatformConnectedClient;
import org.terracotta.monitoring.PlatformEntity;
import org.terracotta.monitoring.PlatformServer;
import org.terracotta.monitoring.ServerState;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.terracotta.management.tms.entity.server.PlatformNotificationType.CONNECTION_CLOSED;
import static org.terracotta.management.tms.entity.server.PlatformNotificationType.CONNECTION_OPENED;
import static org.terracotta.management.tms.entity.server.Utils.array;
import static org.terracotta.management.tms.entity.server.Utils.toClientEndpoint;
import static org.terracotta.management.tms.entity.server.Utils.toClientIdentifier;
import static org.terracotta.monitoring.PlatformMonitoringConstants.CLIENTS_ROOT_NAME;
import static org.terracotta.monitoring.PlatformMonitoringConstants.ENTITIES_ROOT_NAME;
import static org.terracotta.monitoring.PlatformMonitoringConstants.PLATFORM_ROOT_NAME;

/**
 * @author Mathieu Carbou
 */
class MutationReducer {

  private final List<Notification> notifications;
  private final Stripe stripe;
  private final Server active;
  private final Cluster cluster;
  private final IMonitoringConsumer consumer;
  private final Map<ClientIdentifier, List<Notification>> clientConnectionNotifications;

  public MutationReducer(IMonitoringConsumer consumer, Cluster cluster, List<Notification> notifications) {
    this.consumer = consumer;
    this.cluster = cluster;
    this.stripe = cluster.getStripes().values().iterator().next();
    this.active = stripe.getActiveServer().orElseThrow(() -> new IllegalStateException("Unable to find active server on stripe " + stripe.getName()));
    this.notifications = notifications;
    // creates a list of all connection notifications in order of arrival, related to one client
    this.clientConnectionNotifications = notifications.stream()
        .filter(notification -> notification.isAnyType(CONNECTION_OPENED, CONNECTION_CLOSED))
        .collect(Collectors.groupingBy(notification -> toClientIdentifier((PlatformConnectedClient) notification.getValue())));
  }

  public Stream<Notification> stream() {
    return notifications.stream();
  }

  public void reduce() {
    // determine for all notifications about client connections if there has been some client disconnection or connection
    // these events do not exist on voltron because they are "logical" events related to a cluster client
    for (List<Notification> connectionNotifications : clientConnectionNotifications.values()) {
      for (int i = 0, max = connectionNotifications.size(); i < max; i++) {
        Notification notification = connectionNotifications.get(i);
        PlatformConnectedClient connection = (PlatformConnectedClient) notification.getValue();
        ClientIdentifier clientIdentifier = toClientIdentifier(connection);
        Context clientContext = Context.create(Client.KEY, clientIdentifier.getClientId());
        notification.setContext(clientContext.with("endpoint", toClientEndpoint(connection).toString()));
        int clientConnectionCount = cluster.getClient(clientIdentifier).map(Client::getConnectionCount).orElse(0);

        if (i == max - 1 && clientConnectionCount == 0 && notification.isAnyType(CONNECTION_CLOSED)) {
          // we assume a disconnection if the current topology does not contain this client
          // and a CONNECTION_CLOSED notification is found at the end
          insertAfter(notification, "CLIENT_DISCONNECTED", clientContext);

          // if we disconnected, check if we had an open event that was done also within the same mutation interval
          // I.e. this is the case where a client quickly join and leaves: we do not hav it anymore in the topology
          connectionNotifications.stream()
              .limit(i)
              .filter(n -> n.isAnyType(CONNECTION_OPENED))
              .findFirst()
              .ifPresent(openedNotif -> insertAfter(openedNotif, "CLIENT_CONNECTED", clientContext));

        } else if (i == max - 1 && clientConnectionCount == 1 && notification.isAnyType(CONNECTION_OPENED)) {
          // if the latest event is a connection open, and the current topology has 1 connection,
          // then it is a client connection
          insertAfter(notification, "CLIENT_CONNECTED", clientContext);
        }
      }
    }

    // keep iteration with index because the list is updated within this loop
    for (int i = 0; i < notifications.size(); i++) {
      Notification notification = notifications.get(i);

      switch (notification.getPlatformNotificationType()) {

        case SERVER_ENTITY_CREATED:
        case SERVER_ENTITY_DESTROYED: {
          PlatformEntity platformEntity = (PlatformEntity) notification.getValue();
          notification.setContext(active.getContext()
              .with(ServerEntity.create(platformEntity.name, platformEntity.typeName).getContext()));
          break;
        }

        case SERVER_JOINED:
        case SERVER_LEFT: {
          String serverName = ((PlatformServer) notification.getValue()).getServerName();
          notification.setContext(stripe.getContext()
              .with(Server.KEY, serverName)
              .with(Server.NAME_KEY, serverName));
          break;
        }

        case SERVER_STATE_CHANGED: {
          PlatformServer platformServer = (PlatformServer) notification.getParentValue(2); // platform/servers/<id>/state
          notification.setContext(stripe.getContext()
              .with(Server.KEY, platformServer.getServerName())
              .with(Server.NAME_KEY, platformServer.getServerName()));
          ServerState serverState = (ServerState) notification.getNewValue();
          notification.setAttribute("state", serverState.getState());
          notification.setAttribute("activateTime", serverState.getActivate() > 0 ? String.valueOf(serverState.getActivate()) : "0");
          break;
        }

        case SERVER_ENTITY_FETCHED:
        case SERVER_ENTITY_UNFETCHED: {
          PlatformClientFetchedEntity fetchedEntity = (PlatformClientFetchedEntity) notification.getValue();
          notification.setContext(getPlatformEntityEntityContext(fetchedEntity.entityIdentifier));
          // add client information if we can
          getPlatformConnectedClient(fetchedEntity.clientIdentifier)
              .map(connection -> toClientIdentifier(connection).getClientId())
              .ifPresent(clientId -> notification.setAttribute(Client.KEY, clientId));
          break;
        }

        case CLIENT_TAGS_UPDATED: {
          // management/clients/<client-identifier>/tags
          ClientIdentifier clientIdentifier = ClientIdentifier.valueOf(notification.getPath(2));
          String[] tags = (String[]) notification.getValue();
          Context clientContext = Context
              .create(Client.KEY, clientIdentifier.getClientId())
              .with("tags", String.join(";", Arrays.asList(tags)));
          insertAfter(notification, "CLIENT_UPDATED", clientContext);
          break;
        }

        case CLIENT_CAPABILITIES_UPDATED:
        case CLIENT_CONTEXT_CONTAINER_UPDATED: {
          // management/clients/<client-identifier>/registry/contextContainer
          // management/clients/<client-identifier>/registry/capabilities
          ClientIdentifier clientIdentifier = ClientIdentifier.valueOf(notification.getPath(2));
          Context clientContext = Context
              .create(Client.KEY, clientIdentifier.getClientId());
          insertAfter(notification, "CLIENT_REGISTRY_UPDATED", clientContext);
          break;
        }

        case CONNECTION_OPENED:
        case CONNECTION_CLOSED:
        case OTHER:
          // do nothing for these events
          // they were handled before
          break;

        default:
          throw new AssertionError(notification.getType());
      }
    }
  }

  private void insertAfter(Notification notification, String notifType, Context context) {
    // create notification from a copy
    Notification copy = notification.copy();
    copy.setType(notifType);
    copy.setContext(context);

    // insert it
    int idx = notifications.indexOf(notification);
    notifications.add(idx + 1, copy);

    // cleanup duplicated ones that were set before
    Iterator<Notification> it = notifications.iterator();
    while (it.hasNext()) {
      Notification next = it.next();
      if (next == notification) {
        break;
      }
      if (notifType.equals(next.getType()) && context.equals(next.getContext())) {
        it.remove();
      }
    }
  }

  private Context getPlatformEntityEntityContext(String entityIdentifier) {
    return getPlatformEntity(entityIdentifier)
        .map(platformEntity -> active.getContext()
            .with(ServerEntity.create(platformEntity.name, platformEntity.typeName).getContext()))
        .orElseThrow(() -> new NoSuchElementException("Entity " + entityIdentifier + " not found in server monitoring tree or in tree mutations"));
  }

  private Optional<PlatformConnectedClient> getPlatformConnectedClient(String connectionIdentifier) {
    // first search the connectionId in the current voltron tree or search in the mutation list if not in the tree
    return Optional.ofNullable(consumer.getValueForNode(array(PLATFORM_ROOT_NAME, CLIENTS_ROOT_NAME, connectionIdentifier), PlatformConnectedClient.class)
        .orElseGet(() -> notifications.stream()
            .filter(notif -> notif.pathMatches(PLATFORM_ROOT_NAME, CLIENTS_ROOT_NAME, connectionIdentifier))
            .map(notif -> (PlatformConnectedClient) notif.getValue())
            .findFirst()
            .orElse(null)));
  }

  private Optional<PlatformEntity> getPlatformEntity(String entityIdentifier) {
    // first search the connectionId in the current voltron tree or search in the mutation list if not in the tree
    return Optional.ofNullable(consumer.getValueForNode(array(PLATFORM_ROOT_NAME, ENTITIES_ROOT_NAME, entityIdentifier), PlatformEntity.class)
        .orElseGet(() -> notifications.stream()
            .filter(notif -> notif.pathMatches(PLATFORM_ROOT_NAME, ENTITIES_ROOT_NAME, entityIdentifier))
            .map(notif -> (PlatformEntity) notif.getValue())
            .findFirst()
            .orElse(null)));
  }

}
