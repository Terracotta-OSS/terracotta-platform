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
import org.terracotta.monitoring.PlatformConnectedClient;

import static org.terracotta.management.entity.tms.server.Utils.toClientIdentifier;

/**
 * @author Mathieu Carbou
 */
class TopologyBuilder {

  private static final String MANAGEMENT_ENTITY_TYPE = "org.terracotta.management.entity.management.client.ManagementAgentEntity";

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
    consumer.getPlatformServers()
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
        .forEach(server -> {
          // get the server state
          consumer.getServerState(server.getServerName())
              .ifPresent(serverState -> server
                  .setState(Server.State.parse(serverState.getState()))
                  .setActivateTime(serverState.getActivate()));
          // add the server to the stripe
          stripe.addServer(server);
        });

    // only continue if we have an active server where to report client connections and manageable:
    stripe.getActiveServer().ifPresent(active -> {

      // find all entities and associate them to the active
      consumer.getPlatformEntities()
          //TODO: MATHIEU: call .setManagementRegistry() to add management registry on server entities
          .map(platformEntity -> ServerEntity.create(platformEntity.name, platformEntity.typeName, platformEntity.consumerID))
          .forEach(active::addServerEntity);

      // iterate over all connections to create clients and connections to the active
      consumer.getPlatformConnectedClients()
          .forEach(platformClientConnection -> {

            ClientIdentifier clientIdentifier = toClientIdentifier(platformClientConnection);
            Endpoint endpoint = toClientEndpoint(platformClientConnection);
            Connection connection = Connection.create(clientIdentifier.getConnectionUid(), active, endpoint);

            // create the client and adds the connection
            Client client = cluster.getClient(clientIdentifier).orElseGet(() -> {
              Client c = toClient(platformClientConnection);
              cluster.addClient(c);
              return c;
            });
            client.addConnection(connection);

            // associate created server entities to connected clients
            consumer.getFetchedEntities(platformClientConnection)
                .forEach(platformEntity -> connection.fetchServerEntity(platformEntity.name, platformEntity.typeName));

            // try to find some management information for this client in all possible management trees
            consumer.getMonitoringTrees(MANAGEMENT_ENTITY_TYPE)
                .filter(monitoringTree -> monitoringTree.containsPath("management", "clients", client.getClientId()))
                .forEach(monitoringTree -> {

                  // find client tags
                  monitoringTree.getValueForNode(array("management", "clients", client.getClientId(), "tags"), String[].class)
                      .ifPresent(client::addTags);

                  // find all management registry metadata exposed client-side through the "org.terracotta.management.entity.management.client.ManagementAgentEntity"
                  monitoringTree.getValueForNode(array("management", "clients", client.getClientId(), "registry", "contextContainer"), ContextContainer.class)
                      .ifPresent(contextContainer -> {

                        // create a management registry
                        ManagementRegistry registry = ManagementRegistry.create(contextContainer);
                        client.setManagementRegistry(registry);

                        // then add the capabilities if there are some
                        monitoringTree.getValueForNode(array("management", "clients", client.getClientId(), "registry", "capabilities"), Capability[].class)
                            .ifPresent(registry::setCapabilities);
                      });
                });
          });
    });

    return cluster;
  }

  private static String[] array(String... o) {
    return o;
  }

  private static Endpoint toClientEndpoint(PlatformConnectedClient connection) {
    return Endpoint.create(connection.remoteAddress.getHostAddress(), connection.remotePort);
  }

  private static Client toClient(PlatformConnectedClient connection) {
    ClientIdentifier clientIdentifier = toClientIdentifier(connection);
    return Client.create(clientIdentifier)
        .setHostName(connection.remoteAddress.getHostName());
  }

}
