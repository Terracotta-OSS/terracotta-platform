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
package org.terracotta.dynamic_config.server.configuration.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.ClusterState;
import org.terracotta.dynamic_config.api.model.FailoverPriority;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.nomad.DynamicConfigNomadChange;
import org.terracotta.dynamic_config.api.service.ClusterValidator;

import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.lang.System.lineSeparator;
import static java.util.Objects.requireNonNull;
import static org.terracotta.dynamic_config.api.model.FailoverPriority.Type.CONSISTENCY;

/**
 * @author Mathieu Carbou
 */
class Topologies {

  private static final Logger LOGGER = LoggerFactory.getLogger(Topologies.class);

  // guard access to the topologies
  private final ReadWriteLock topologyLock = new ReentrantReadWriteLock();

  private NodeContext upcomingNodeContext;
  private NodeContext runtimeNodeContext;

  Topologies(NodeContext nodeContext) {
    upcomingNodeContext = runtimeNodeContext = requireNonNull(nodeContext);
  }

  public void withUpcoming(Consumer<NodeContext> c) {
    topologyLock.readLock().lock();
    try {
      c.accept(upcomingNodeContext);
    } finally {
      topologyLock.readLock().unlock();
    }
  }

  public NodeContext getUpcomingNodeContext() {
    topologyLock.readLock().lock();
    try {
      return upcomingNodeContext.clone();
    } finally {
      topologyLock.readLock().unlock();
    }
  }

  public NodeContext getRuntimeNodeContext() {
    topologyLock.readLock().lock();
    try {
      return runtimeNodeContext.clone();
    } finally {
      topologyLock.readLock().unlock();
    }
  }

  public boolean areSame() {
    topologyLock.readLock().lock();
    try {
      return runtimeNodeContext.equals(upcomingNodeContext);
    } finally {
      topologyLock.readLock().unlock();
    }
  }

  public NodeContext install(Cluster updatedCluster) {
    requireNonNull(updatedCluster);

    topologyLock.writeLock().lock();
    try {
      new ClusterValidator(updatedCluster).validate(ClusterState.CONFIGURING);

      Node newMe = findMe(updatedCluster);

      if (newMe != null) {
        // we have updated the topology and I am still part of this cluster
        LOGGER.trace("Set upcoming topology to:\n{}", updatedCluster);
        this.upcomingNodeContext = new NodeContext(updatedCluster, newMe.getUID());

      } else {
        // We have updated the topology and I am not part anymore of the cluster
        // So we just reset the cluster object so that this node is alone
        Node oldMe = upcomingNodeContext.getNode();
        LOGGER.info("Node {} ({}) removed from pending topology: {}", oldMe.getName(), oldMe.getUID(), updatedCluster.toShapeString());
        this.upcomingNodeContext = this.upcomingNodeContext.withOnlyNode(oldMe);
      }

      // When node is not yet activated, runtimeNodeContext == upcomingNodeContext
      this.runtimeNodeContext = upcomingNodeContext;
      return upcomingNodeContext;

    } finally {
      topologyLock.writeLock().unlock();
    }
  }

  /**
   * Tries to find the node representing this process within the updated cluster.
   * <p>
   * - We cannot use the node hostname or port only, since they might have changed through a set command.
   * - We cannot use the node name and stripe ID only, since the stripe ID can have changed in the new cluster with the attach/detach commands
   * - Name could change following a set command when un-configured
   * <p>
   * So we try to find the best match we can...
   */
  private Node findMe(Cluster updatedCluster) {
    final Node me = getUpcomingNodeContext().getNode();
    for (Node node : updatedCluster.getNodes()) {
      if (node.getUID().equals(me.getUID())
          || node.getInternalHostPort().equals(me.getInternalHostPort())
          || node.getName().equals(me.getName())) {
        return node;
      }
    }
    return null;
  }

  public boolean containsMe(Cluster maybeUpdatedCluster) {
    return findMe(maybeUpdatedCluster) != null;
  }

  public void update(List<? extends DynamicConfigNomadChange> nomadChanges) {
    // the following code will be executed on all the nodes, regardless of the applicability
    // level to update the config
    topologyLock.writeLock().lock();
    try {
      for (DynamicConfigNomadChange nomadChange : nomadChanges) {
        // first we update the upcoming one
        Cluster upcomingCluster = nomadChange.apply(upcomingNodeContext.getCluster());
        upcomingNodeContext = upcomingNodeContext.withCluster(upcomingCluster).orElseGet(upcomingNodeContext::alone);
        // if the change can be applied at runtime, it was previously done in the config change handler.
        // so update also the runtime topology there
        if (nomadChange.canUpdateRuntimeTopology(runtimeNodeContext)) {
          Cluster runtimeCluster = nomadChange.apply(runtimeNodeContext.getCluster());
          runtimeNodeContext = runtimeNodeContext.withCluster(runtimeCluster).orElseGet(runtimeNodeContext::alone);
        }
      }
    } finally {
      topologyLock.writeLock().unlock();
    }
  }

  public void warnIfProblematicConsistency() {
    final NodeContext nodeContext = getUpcomingNodeContext();
    FailoverPriority failover = nodeContext.getCluster().getFailoverPriority().orElse(null);
    if (failover != null && failover.getType() == CONSISTENCY) {
      int voters = failover.getVoters();
      List<Stripe> evenNodeStripes = nodeContext.getCluster().getStripes()
          .stream().filter(s -> (s.getNodeCount() + voters) % 2 == 0).collect(Collectors.toList());
      if (evenNodeStripes.size() > 0) {
        StringBuilder warn = new StringBuilder(lineSeparator());
        warn.append("===================================================================================================================").append(lineSeparator());
        warn.append("When a cluster is configured with failover-priority=consistency, stripes with an even number").append(lineSeparator());
        warn.append("of nodes plus voters are more likely to experience split brain situations.").append(lineSeparator());
        warn.append("The following stripe(s) have an even number of nodes plus voters:").append(lineSeparator());
        for (Stripe s : evenNodeStripes) {
          warn.append("   Stripe: '").append(s.getName()).append("' has ").append(s.getNodeCount()).append(" nodes and ").append(voters).append(" voters.").append(lineSeparator());
        }
        warn.append("===================================================================================================================").append(lineSeparator());
        LOGGER.warn(warn.toString());
      }
    }
  }
}
