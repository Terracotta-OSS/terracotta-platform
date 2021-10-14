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

import java.util.Objects;
import java.util.Optional;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

/**
 * @author Mathieu Carbou
 */
public class NodeContext implements Cloneable {

  private final Cluster cluster;
  private final UID nodeUID;
  private final Node node;
  private final Stripe stripe;

  public NodeContext(Cluster cluster, UID nodeUID) {
    requireNonNull(nodeUID);
    this.nodeUID = requireNonNull(nodeUID);
    this.cluster = requireNonNull(cluster);
    this.node = cluster.getNode(nodeUID)
        .orElseThrow(() -> new IllegalArgumentException("Node UID: " + nodeUID + " not found in cluster: " + cluster.toShapeString()));
    this.stripe = cluster.getStripeByNode(nodeUID).get();
  }

  public Cluster getCluster() {
    return cluster;
  }

  public UID getNodeUID() {
    return nodeUID;
  }

  public Node getNode() {
    return node;
  }

  public Stripe getStripe() {
    return stripe;
  }

  public UID getStripeUID() {
    return stripe.getUID();
  }

  public Optional<String> getProperty(Setting setting) {
    return setting.getProperty(getObject(setting.getScope()));
  }

  public PropertyHolder getObject(Scope scope) {
    switch (scope) {
      case CLUSTER:
        return getCluster();
      case STRIPE:
        return getStripe();
      case NODE:
        return getNode();
      default:
        throw new AssertionError(scope);
    }
  }

  @Override
  @SuppressWarnings("MethodDoesntCallSuperMethod")
  @SuppressFBWarnings("CN_IDIOM_NO_SUPER_CALL")
  public NodeContext clone() {
    return new NodeContext(cluster.clone(), nodeUID);
  }

  @Override
  public String toString() {
    return "NodeContext{" +
        "cluster=" + cluster.toShapeString() +
        ", nodeUID=" + nodeUID +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof NodeContext)) return false;
    NodeContext that = (NodeContext) o;
    return getCluster().equals(that.getCluster()) &&
        getNodeUID().equals(that.getNodeUID());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getCluster(), getNodeUID());
  }

  /**
   * Rebase this node information onto another cluster.
   * <p>
   * Parameter must not be null.
   * <p>
   * If the new cluster contains this node UID or name or address,
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

    // find by UID
    return updated.containsNode(nodeUID) ?
        Optional.of(new NodeContext(updated, nodeUID)) :
        // find by name
        updated.containsNode(node.getName()) ?
            Optional.of(new NodeContext(updated, updated.getNodeByName(node.getName()).get().getUID())) :
            // find by internal address (which never changes)
            updated.getNodes().stream()
                .filter(n -> n.getInternalSocketAddress().equals(node.getInternalSocketAddress()))
                .map(n -> new NodeContext(updated, n.getUID()))
                .findAny();
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
    Stripe stripe = getStripe().clone().setNodes(singletonList(node));
    Cluster cluster = getCluster().clone().setStripes(singletonList(stripe));
    return new NodeContext(cluster, node.getUID());
  }
}
