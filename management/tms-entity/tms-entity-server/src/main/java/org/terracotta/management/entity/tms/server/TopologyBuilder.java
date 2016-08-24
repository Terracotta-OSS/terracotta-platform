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
package org.terracotta.management.entity.tms.server;


import org.terracotta.management.model.capabilities.Capability;
import org.terracotta.management.model.cluster.Client;
import org.terracotta.management.model.cluster.ClientIdentifier;
import org.terracotta.management.model.cluster.Cluster;
import org.terracotta.management.model.cluster.Connection;
import org.terracotta.management.model.cluster.Endpoint;
import org.terracotta.management.model.cluster.ManagementRegistry;
import org.terracotta.management.model.cluster.Server;
import org.terracotta.management.model.cluster.ServerEntity;
import org.terracotta.management.model.cluster.Stripe;
import org.terracotta.management.model.context.ContextContainer;
import org.terracotta.management.service.monitoring.IMonitoringConsumer;
import org.terracotta.monitoring.PlatformClientFetchedEntity;
import org.terracotta.monitoring.PlatformConnectedClient;
import org.terracotta.monitoring.PlatformEntity;
import org.terracotta.monitoring.PlatformServer;
import org.terracotta.monitoring.ServerState;

import java.util.Optional;

import static org.terracotta.management.entity.tms.server.Utils.array;
import static org.terracotta.management.entity.tms.server.Utils.toClient;
import static org.terracotta.management.entity.tms.server.Utils.toClientEndpoint;
import static org.terracotta.management.entity.tms.server.Utils.toClientIdentifier;
import static org.terracotta.monitoring.PlatformMonitoringConstants.CLIENTS_PATH;
import static org.terracotta.monitoring.PlatformMonitoringConstants.CLIENTS_ROOT_NAME;
import static org.terracotta.monitoring.PlatformMonitoringConstants.ENTITIES_PATH;
import static org.terracotta.monitoring.PlatformMonitoringConstants.ENTITIES_ROOT_NAME;
import static org.terracotta.monitoring.PlatformMonitoringConstants.FETCHED_PATH;
import static org.terracotta.monitoring.PlatformMonitoringConstants.PLATFORM_ROOT_NAME;
import static org.terracotta.monitoring.PlatformMonitoringConstants.SERVERS_PATH;
import static org.terracotta.monitoring.PlatformMonitoringConstants.SERVERS_ROOT_NAME;
import static org.terracotta.monitoring.PlatformMonitoringConstants.STATE_NODE_NAME;

/**
 * @author Mathieu Carbou
 */
class TopologyBuilder {

  private final IMonitoringConsumer consumer;
  private final String stripeName;

  public TopologyBuilder(IMonitoringConsumer consumer, String stripeName) {
    this.consumer = consumer;
    this.stripeName = stripeName;
  }

  @SuppressWarnings("unchecked")
  public Cluster buildTopology() {
    Cluster cluster = Cluster.create();
    Stripe stripe = Stripe.create(stripeName);
    cluster.addStripe(stripe);

    // get servers
    consumer.getChildNamesForNode(SERVERS_PATH).ifPresent(nodeIds -> {
      for (String nodeId : nodeIds) {

        consumer.getValueForNode(SERVERS_PATH, nodeId, PlatformServer.class)
            .map(platformServer -> Server.create(platformServer.getServerName())
                .setBindAddress(platformServer.getBindAddress())
                .setBindPort(platformServer.getBindPort())
                .setBuildId(platformServer.getBuild())
                .setGroupPort(platformServer.getGroupPort())
                .setHostName(platformServer.getHostName())
                .setStartTime(platformServer.getStartTime())
                .setHostAddress(platformServer.getHostAddress())
                .setVersion(platformServer.getVersion())
                .computeUpTime())
            .ifPresent(server -> {

              // also try to determine its state
              consumer.getValueForNode(array(PLATFORM_ROOT_NAME, SERVERS_ROOT_NAME, nodeId, STATE_NODE_NAME), ServerState.class)
                  .ifPresent(serverState -> server
                      .setState(org.terracotta.management.model.cluster.ServerState.parse(serverState.getState()))
                      .setActivateTime(serverState.getActivate()));

              // add the server to the stripe
              stripe.addServer(server);
            });
      }
    });

    // only continue if we have an active server where to report client connections and manageable:
    stripe.getActiveServer().ifPresent(active -> {

      // find all entities and associate them to the active
      consumer.getChildNamesForNode(ENTITIES_PATH).ifPresent(entityIdentifiers -> {
        for (String entityIdentifier : entityIdentifiers) {

          // create server entity and associate it to a server
          consumer.getValueForNode(ENTITIES_PATH, entityIdentifier, PlatformEntity.class)
              //TODO: MATHIEU: call .setManagementRegistry() to add management registry on server entities
              .map(platformEntity -> ServerEntity.create(platformEntity.name, platformEntity.typeName))
              .ifPresent(active::addServerEntity);
        }
      });

      // iterate over all connections to create clients and connections to the active
      consumer.getChildNamesForNode(CLIENTS_PATH).ifPresent(connectionIdentifiers -> {
        for (String connectionIdentifier : connectionIdentifiers) {

          getPlatformConnectedClient(connectionIdentifier).ifPresent(connection -> {
            ClientIdentifier clientIdentifier = toClientIdentifier(connection);

            Client client = cluster.getClient(clientIdentifier).orElseGet(() -> {
              Client newClient = toClient(connection);
              cluster.addClient(newClient);
              return newClient;
            });

            // find client tags
            consumer.getValueForNode(array("management", "clients", client.getClientId(), "tags"), String[].class)
                .ifPresent(client::addTags);

            // find all management registry metadata exposed client-side through the "org.terracotta.management.entity.management.client.ManagementAgentEntity"
            consumer.getValueForNode(array("management", "clients", client.getClientId(), "registry", "contextContainer"), ContextContainer.class)
                .ifPresent(contextContainer -> {
                  ManagementRegistry registry = ManagementRegistry.create(contextContainer);

                  client.setManagementRegistry(registry);

                  // then add the capabilities if there are some
                  consumer.getValueForNode(array("management", "clients", client.getClientId(), "registry", "capabilities"), Capability[].class)
                      .ifPresent(registry::setCapabilities);
                });

            Endpoint endpoint = toClientEndpoint(connection);
            client.addConnection(Connection.create(clientIdentifier.getConnectionUid(), active, endpoint));
          });
        }
      });

      // associate created server entities to connected clients
      consumer.getChildNamesForNode(FETCHED_PATH).ifPresent(fetchIdentifiers -> {
        for (String fetchIdentifier : fetchIdentifiers) {

          consumer.getValueForNode(FETCHED_PATH, fetchIdentifier, PlatformClientFetchedEntity.class)
              .ifPresent(platformClientFetchedEntity -> {

                // get the server entity
                getPlatformEntity(platformClientFetchedEntity.entityIdentifier)
                    .flatMap(platformEntity -> active.getServerEntity(platformEntity.name, platformEntity.typeName))
                    .ifPresent(serverEntity -> {

                      // get the client connection connected to teh active server
                      getPlatformConnectedClient(platformClientFetchedEntity.clientIdentifier)
                          .flatMap(connection -> cluster.getClient(toClientIdentifier(connection))
                              .flatMap(client -> client.getConnection(active, toClientEndpoint(connection))))
                          // and associate the manageable
                          .ifPresent(connection -> connection.fetchServerEntity(serverEntity));
                    });
              });
        }
      });
    });

    return cluster;
  }

  private Optional<PlatformConnectedClient> getPlatformConnectedClient(String connectionIdentifier) {
    return consumer.getValueForNode(array(PLATFORM_ROOT_NAME, CLIENTS_ROOT_NAME, connectionIdentifier), PlatformConnectedClient.class);
  }

  private Optional<PlatformEntity> getPlatformEntity(String entityIdentifier) {
    return consumer.getValueForNode(array(PLATFORM_ROOT_NAME, ENTITIES_ROOT_NAME, entityIdentifier), PlatformEntity.class);
  }

}
