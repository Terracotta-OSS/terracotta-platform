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
package org.terracotta.dynamic_config.api.model;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * @author Mathieu Carbou
 */
public class NodeContext implements Cloneable {

  private final Cluster cluster;
  private final int stripeId;
  private final int nodeId;
  private final String nodeName;
  private final Node node;

  public NodeContext(Cluster cluster,
                     int stripeId,
                     String nodeName) {
    this.cluster = requireNonNull(cluster);
    this.stripeId = stripeId;
    this.nodeName = requireNonNull(nodeName);
    if (stripeId < 1 || stripeId > cluster.getStripeCount()) {
      throw new IllegalArgumentException("Invalid stripe ID: " + stripeId);
    }
    this.node = cluster.getNode(stripeId, nodeName)
        .orElseThrow(() -> new IllegalArgumentException("Node " + nodeName + " in stripe ID " + stripeId + " not found"));
    this.nodeId = cluster.getNodeId(stripeId, nodeName)
        .orElseThrow(() -> new IllegalArgumentException("Node " + nodeName + " in stripe ID " + stripeId + " not found"));
  }

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  public NodeContext(Cluster cluster, InetSocketAddress nodeAddress) {
    requireNonNull(nodeAddress);
    this.cluster = requireNonNull(cluster);
    this.node = cluster.getNode(nodeAddress)
        .orElseThrow(() -> new IllegalArgumentException("Node " + nodeAddress + " not found"));
    this.nodeName = requireNonNull(node.getName());
    this.stripeId = cluster.getStripeId(nodeAddress).getAsInt();
    this.nodeId = cluster.getNodeId(nodeAddress).getAsInt();
  }

  public NodeContext(Cluster cluster, int stripeId, int nodeId) {
    this.cluster = requireNonNull(cluster);
    this.stripeId = stripeId;
    this.nodeId = nodeId;
    this.node = cluster.getNode(stripeId, nodeId)
        .orElseThrow(() -> new IllegalArgumentException("Node ID " + nodeId + " in stripe ID " + stripeId + " not found"));
    this.nodeName = requireNonNull(node.getName());
  }

  public Cluster getCluster() {
    return cluster;
  }

  public int getStripeId() {
    return stripeId;
  }

  public int getNodeId() {
    return nodeId;
  }

  public String getNodeName() {
    return nodeName;
  }

  public Node getNode() {
    return node;
  }

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  public Stripe getStripe() {
    return cluster.getStripe(stripeId).get();
  }

  @Override
  @SuppressWarnings("MethodDoesntCallSuperMethod")
  @SuppressFBWarnings("CN_IDIOM_NO_SUPER_CALL")
  public NodeContext clone() {
    return new NodeContext(cluster.clone(), stripeId, nodeName);
  }

  @Override
  public String toString() {
    return "NodeContext{" + "cluster=" + cluster.toShapeString() +
        ", stripeId=" + stripeId +
        ", nodeId=" + nodeId +
        ", nodeName='" + nodeName + '\'' +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof NodeContext)) return false;
    NodeContext that = (NodeContext) o;
    return getStripeId() == that.getStripeId() &&
        getNodeId() == that.getNodeId() &&
        getCluster().equals(that.getCluster()) &&
        getNodeName().equals(that.getNodeName());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getCluster(), getStripeId(), getNodeId(), getNodeName());
  }

  /**
   * Rebase this node information onto another cluster.
   * <p>
   * Parameter must not be null.
   * <p>
   * If the new cluster contains this stripe ID / node name information,
   * then a new node context is returned targeting the same node in the new cluster.
   * <p>
   * Otherwise, an empty optional is returned
   */
  public Optional<NodeContext> withCluster(Cluster updated) {
    requireNonNull(updated);

    // The base config comes from a loaded config xml file.
    // The config xml file must be for this node.
    // This is a programming issue otherwise

    // If the updated topology does not contain the node anymore (removal ?) and a base config was there (topology change)
    // then we isolate the node in its own cluster

    return updated.containsNode(stripeId, nodeName) ?
        Optional.of(new NodeContext(updated, stripeId, nodeName)) :
        Optional.empty();
  }

  /**
   * Return a node context where the current node will be alone in a new cluster.
   * This can be useful to clear out all other nodes in case we need to simulate
   * starting a cluster in repair mode
   */
  public NodeContext alone() {
    return withOnlyNode(getNode());
  }

  /**
   * Returns this cluster with this node only
   */
  public NodeContext withOnlyNode(Node node) {
    Cluster cluster = getCluster().clone().removeStripes();
    cluster.addStripe(new Stripe(node).clone());
    return new NodeContext(cluster, node.getAddress());
  }
}
