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
package org.terracotta.dynamic_config.api.model.nomad;

import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.Scope;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.UID;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

import static java.util.Objects.requireNonNull;
import static org.terracotta.dynamic_config.api.model.Scope.CLUSTER;
import static org.terracotta.dynamic_config.api.model.Scope.NODE;
import static org.terracotta.dynamic_config.api.model.Scope.STRIPE;

public interface Applicability {

  static Applicability cluster() {
    return new V2(CLUSTER, null, null);
  }

  static Applicability stripe(UID stripeUID) {
    return new V2(STRIPE, requireNonNull(stripeUID), null);
  }

  static Applicability node(UID nodeUID) {
    return new V2(NODE, null, requireNonNull(nodeUID));
  }

  Scope getLevel();

  Optional<Stripe> getStripe(Cluster cluster);

  Optional<Node> getNode(Cluster cluster);

  boolean isApplicableTo(NodeContext node);

  class V2 implements Applicability {
    private final Scope level;
    private final UID nodeUID;
    private final UID stripeUID;

    // For Json
    private V2() {
      level = null;
      nodeUID = null;
      stripeUID = null;
    }

    public V2(Scope level, UID stripeUID, UID nodeUID) {
      this.level = requireNonNull(level);
      this.stripeUID = stripeUID;
      this.nodeUID = nodeUID;
    }

    public Scope getLevel() {
      return level;
    }

    public UID getNodeUID() {
      return nodeUID;
    }

    public UID getStripeUID() {
      return stripeUID;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o instanceof Applicability
          && ((Applicability) o).getLevel() == CLUSTER
          && getLevel() == CLUSTER) {
        return true;
      }
      if (!(o instanceof V2)) return false;
      V2 that = (V2) o;
      return getLevel() == that.getLevel()
          && Objects.equals(getNodeUID(), that.getNodeUID())
          && Objects.equals(getStripeUID(), that.getStripeUID());
    }

    @Override
    public int hashCode() {
      return Objects.hash(getLevel(), getNodeUID(), getStripeUID());
    }

    @Override
    public String toString() {
      switch (getLevel()) {
        case STRIPE:
          return "stripe UID: " + getStripeUID();
        case NODE:
          return "node UID: " + getNodeUID();
        default:
          return "cluster";
      }
    }

    @Override
    public Optional<Stripe> getStripe(Cluster cluster) {
      switch (level) {
        case CLUSTER:
          throw new UnsupportedOperationException("level: " + level);
        case STRIPE:
          return cluster.getStripe(stripeUID);
        case NODE:
          return cluster.getStripeByNode(nodeUID);
        default:
          throw new AssertionError(level);
      }
    }

    @Override
    public Optional<Node> getNode(Cluster cluster) {
      return cluster.getNode(getNodeUID());
    }

    @Override
    public boolean isApplicableTo(NodeContext node) {
      switch (getLevel()) {
        case CLUSTER:
          return true;
        case STRIPE:
          return getStripeUID().equals(node.getStripeUID());
        case NODE:
          return getNodeUID().equals(node.getNodeUID());
        default:
          throw new AssertionError(getLevel());
      }
    }
  }

  /**
   * This class is for backward compatibility json deserializing and serializing with the V1 json format
   */
  class V1 implements Applicability {
    private final Scope scope;
    private final String nodeName;
    private final Integer stripeId;

    // For Json
    private V1() {
      scope = null;
      nodeName = null;
      stripeId = null;
    }

    public V1(Scope level, Integer stripeId, String nodeName) {
      this.scope = requireNonNull(level);
      this.stripeId = stripeId;
      this.nodeName = nodeName;
    }

    public Scope getLevel() {
      return scope;
    }

    public String getNodeName() {
      return nodeName;
    }

    public OptionalInt getStripeId() {
      return stripeId == null ? OptionalInt.empty() : OptionalInt.of(stripeId);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o instanceof Applicability
          && ((Applicability) o).getLevel() == CLUSTER
          && getLevel() == CLUSTER) {
        return true;
      }
      if (!(o instanceof V1)) return false;
      V1 that = (V1) o;
      return getLevel() == that.getLevel()
          && Objects.equals(getNodeName(), that.getNodeName())
          && Objects.equals(getStripeId(), that.getStripeId());
    }

    @Override
    public int hashCode() {
      return Objects.hash(getLevel(), getNodeName(), getStripeId());
    }

    @Override
    public String toString() {
      switch (getLevel()) {
        case STRIPE:
          return "stripe ID: " + getStripeId().getAsInt();
        case NODE:
          return "stripe ID: " + getStripeId().getAsInt() + ", node: " + getNodeName();
        default:
          return "cluster";
      }
    }

    @Override
    public Optional<Stripe> getStripe(Cluster cluster) {
      return cluster.getStripe(getStripeId().getAsInt());
    }

    @Override
    public Optional<Node> getNode(Cluster cluster) {
      return cluster.getNodeByName(getNodeName());
    }

    @Override
    public boolean isApplicableTo(NodeContext node) {
      switch (getLevel()) {
        case CLUSTER:
          return true;
        case STRIPE:
          return getStripeId().isPresent()
              && node.getCluster().getStripeId(node.getStripeUID()).isPresent()
              && getStripeId().getAsInt() == node.getCluster().getStripeId(node.getStripeUID()).getAsInt();
        case NODE:
          return node.getNode().getName().equals(getNodeName());
        default:
          throw new AssertionError(getLevel());
      }
    }
  }
}
