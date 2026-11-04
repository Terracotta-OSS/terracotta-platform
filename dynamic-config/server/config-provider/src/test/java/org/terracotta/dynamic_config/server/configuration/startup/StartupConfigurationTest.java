/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2026
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
package org.terracotta.dynamic_config.server.configuration.startup;

import org.junit.Test;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.Testing;
import org.terracotta.dynamic_config.api.server.PathResolver;
import org.terracotta.json.DefaultJsonFactory;
import org.terracotta.server.Server;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.function.Supplier;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.terracotta.dynamic_config.api.service.IParameterSubstitutor.identity;

/**
 * Tests for the {@code isPartialConfiguration()} check introduced to put a non-replica
 * node into diagnostic mode when its stripe contains a replica peer.
 */
public class StartupConfigurationTest {

  private final PathResolver pathResolver = new PathResolver(Paths.get(""));

  /**
   * Positive case: the local node is a plain node but a peer in the same stripe is
   * configured as a replica.  The local node must be treated as partial (diagnostic
   * mode) because it is not itself the replica.
   */
  @Test
  public void isPartialConfiguration_returnsTrueWhenStripeContainsReplicaPeer() {
    // Local node – no DR flags
    Node localNode = Testing.newTestNode("node-1", "localhost", 9410, Testing.N_UIDS[1]);

    // Peer node configured as a replica
    Node replicaNode = Testing.newTestNode("node-2", "replica-host", 9411, Testing.N_UIDS[2])
        .setReplica(true)
        .setRelayHostname("relay-host")
        .setRelayPort(9410)
        .setRelayGroupPort(9430);

    Stripe stripe = new Stripe().addNodes(localNode, replicaNode);
    Cluster cluster = Testing.newTestCluster("my-cluster", stripe);
    NodeContext nodeContext = new NodeContext(cluster, localNode.getUID());

    StartupConfiguration config = newStartupConfiguration(() -> nodeContext, false, false);

    assertTrue("isPartialConfiguration should be true when a peer is a replica",
        config.isPartialConfiguration());
  }

  /**
   * Negative case: no node in the stripe is configured as a replica, and neither
   * unConfigured nor repairMode is set.  The node must NOT be treated as partial.
   */
  @Test
  public void isPartialConfiguration_returnsFalseWhenNoReplicaPeerExists() {
    // Local node – no DR flags
    Node localNode = Testing.newTestNode("node-1", "localhost", 9410, Testing.N_UIDS[1]);

    // Peer node with no DR flags at all
    Node normalPeer = Testing.newTestNode("node-2", "peer-host", 9411, Testing.N_UIDS[2]);

    Stripe stripe = new Stripe().addNodes(localNode, normalPeer);
    Cluster cluster = Testing.newTestCluster("my-cluster", stripe);
    NodeContext nodeContext = new NodeContext(cluster, localNode.getUID());

    StartupConfiguration config = newStartupConfiguration(() -> nodeContext, false, false);

    assertFalse("isPartialConfiguration should be false when no peer is a replica",
        config.isPartialConfiguration());
  }

  /**
   * Boundary case: the local node IS the replica itself; it should not trigger the
   * {@code hasReplicaInCluster()} path (that path only fires when a *peer* is a
   * replica).  However, a single-node stripe with the local node as a replica still
   * has no non-local replica peer, so {@code hasReplicaInCluster()} returns false and
   * the only relevant flag is whether {@code unConfigured} or {@code repairMode} is set.
   */
  @Test
  public void isPartialConfiguration_returnsFalseWhenOnlyLocalNodeIsReplica() {
    // Local node IS the replica
    Node replicaNode = Testing.newTestNode("node-1", "localhost", 9410, Testing.N_UIDS[1])
        .setReplica(true)
        .setRelayHostname("relay-host")
        .setRelayPort(9410)
        .setRelayGroupPort(9430);

    Stripe stripe = new Stripe().addNodes(replicaNode);
    Cluster cluster = Testing.newTestCluster("my-cluster", stripe);
    NodeContext nodeContext = new NodeContext(cluster, replicaNode.getUID());

    StartupConfiguration config = newStartupConfiguration(() -> nodeContext, false, false);

    // hasReplicaInCluster() iterates peers only; when the local node is the sole
    // node there are no non-local peers that are replicas, so it returns false.
    assertFalse("isPartialConfiguration should be false when local node itself is the only replica",
        config.isPartialConfiguration());
  }

  // -------------------------------------------------------------------------

  @SuppressWarnings("unchecked")
  private StartupConfiguration newStartupConfiguration(Supplier<NodeContext> supplier,
                                                        boolean unConfigured,
                                                        boolean repairMode) {
    Server server = mock(Server.class);
    when(server.getImplementations(org.terracotta.dynamic_config.api.server.GroupPortMapper.class))
        .thenReturn(Collections.singletonList(DefaultGroupPortMapperImpl.class));
    return new StartupConfiguration(supplier, unConfigured, repairMode,
        getClass().getClassLoader(), pathResolver, identity(), new DefaultJsonFactory(), server);
  }
}
