/*
 * Copyright Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.terracotta.management.model.cluster;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.terracotta.management.model.capabilities.ActionsCapability;
import org.terracotta.management.model.capabilities.Capability;
import org.terracotta.management.model.capabilities.context.CapabilityContext;
import org.terracotta.management.model.context.ContextContainer;

/**
 * @author Mathieu Carbou
 */
@RunWith(JUnit4.class)
public abstract class AbstractTest {

  protected Cluster cluster1;
  protected Cluster cluster2;
  protected Manageable ehcache_client_entity;
  protected Manageable ehcache_server_entity;
  protected Capability action;
  protected ContextContainer contextContainer;
  private ClientIdentifier clientIdentifier;

  @Before
  public void createClusters() {
    action = new ActionsCapability("ActionCapability", new CapabilityContext());
    contextContainer = new ContextContainer("cacheManagerName", "cache-manager-1", new ContextContainer("cacheName", "my-cache"));
    clientIdentifier = ClientIdentifier.create("ehcache", "client1");

    cluster1 = Cluster.create()
        .addStripe(Stripe.create("stripe-1")
            .addServer(Server.create("server-1")
                .setHostName("hostname-1")
                .setBindAddress("0.0.0.0")
                .setBindPort(8881)
                .setState(ServerState.ACTIVE)
                .addManageable(ehcache_server_entity = Manageable.create(contextContainer.getValue(), ManageableType.CACHE_MANAGER_SERVER_ENTITY)
                    .setContextContainer(contextContainer)
                    .addCapability(action))
                .addManageable(Manageable.create("service-1", "SERVICE")
                    .setContextContainer(new ContextContainer("serviceName", "task-service"))))
            .addServer(Server.create("server-2")
                .setHostName("hostname-2")
                .setBindAddress("0.0.0.0")
                .setBindPort(8881)
                .setState(ServerState.PASSIVE)))
        .addStripe(Stripe.create("stripe-2")
            .addServer(Server.create("server-1")
                .setHostName("hostname-3")
                .setBindAddress("0.0.0.0")
                .setBindPort(8881)
                .setState(ServerState.ACTIVE)
                .addManageable(Manageable.create(contextContainer.getValue(), ManageableType.CACHE_MANAGER_SERVER_ENTITY)
                    .setContextContainer(new ContextContainer("cacheManagerName", "cache-manager-1"))
                    .addCapability(action))
                .addManageable(Manageable.create("service-1", "SERVICE")
                    .setContextContainer(new ContextContainer("serviceName", "task-service"))))
            .addServer(Server.create("server-2")
                .setHostName("hostname-4")
                .setBindAddress("0.0.0.0")
                .setBindPort(8881)
                .setState(ServerState.PASSIVE)))
        .addClient(Client.create("12345@127.0.0.1:ehcache:uid")
            .addManageable(ehcache_client_entity = Manageable.create(contextContainer.getValue(), ManageableType.CACHE_MANAGER_SERVER_ENTITY)
                .setContextContainer(contextContainer)
                .addCapability(action)));

    Client client1 = cluster1.getClient("12345@127.0.0.1:ehcache:uid").get();

    client1.addConnection(Connection.create(
        "uid",
        cluster1.getStripe("stripe-1").get().getServerByName("server-1").get(),
        Endpoint.create("10.10.10.10", 3456)));

    client1.addConnection(Connection.create(
        "uid",
        cluster1.getStripe("stripe-2").get().getServerByName("server-1").get(),
        Endpoint.create("10.10.10.10", 3457)));

    cluster2 = Cluster.create()
        .addStripe(Stripe.create("stripe-1")
            .addServer(Server.create("server-1")
                .setHostName("hostname-1")
                .setBindAddress("0.0.0.0")
                .setBindPort(8881)
                .setState(ServerState.ACTIVE)
                .addManageable(ehcache_server_entity = Manageable.create(contextContainer.getValue(), ManageableType.CACHE_MANAGER_SERVER_ENTITY)
                    .setContextContainer(contextContainer)
                    .addCapability(action))
                .addManageable(Manageable.create("service-1", "SERVICE")
                    .setContextContainer(new ContextContainer("serviceName", "task-service"))))
            .addServer(Server.create("server-2")
                .setHostName("hostname-2")
                .setBindAddress("0.0.0.0")
                .setBindPort(8881)
                .setState(ServerState.PASSIVE)))
        .addStripe(Stripe.create("stripe-2")
            .addServer(Server.create("server-1")
                .setHostName("hostname-3")
                .setBindAddress("0.0.0.0")
                .setBindPort(8881)
                .setState(ServerState.ACTIVE)
                .addManageable(Manageable.create(contextContainer.getValue(), ManageableType.CACHE_MANAGER_SERVER_ENTITY)
                    .setContextContainer(new ContextContainer("cacheManagerName", "cache-manager-1"))
                    .addCapability(action))
                .addManageable(Manageable.create("service-1", "SERVICE")
                    .setContextContainer(new ContextContainer("serviceName", "task-service"))))
            .addServer(Server.create("server-2")
                .setHostName("hostname-4")
                .setBindAddress("0.0.0.0")
                .setBindPort(8881)
                .setState(ServerState.PASSIVE)))
        .addClient(Client.create("12345@127.0.0.1:ehcache:uid")
            .addManageable(ehcache_client_entity = Manageable.create(contextContainer.getValue(), ManageableType.CACHE_MANAGER_SERVER_ENTITY)
                .setContextContainer(contextContainer)
                .addCapability(action)));

    Client client2 = cluster2.getClient("12345@127.0.0.1:ehcache:uid").get();

    client2.addConnection(Connection.create(
        "uid",
        cluster2.getStripe("stripe-1").get().getServerByName("server-1").get(),
        Endpoint.create("10.10.10.10", 3456)));

    client2.addConnection(Connection.create(
        "uid",
        cluster2.getStripe("stripe-2").get().getServerByName("server-1").get(),
        Endpoint.create("10.10.10.10", 3457)));
  }

}
