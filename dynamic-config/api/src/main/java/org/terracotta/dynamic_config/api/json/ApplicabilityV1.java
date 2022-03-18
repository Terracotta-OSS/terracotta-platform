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
package org.terracotta.dynamic_config.api.json;

import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.Scope;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.nomad.Applicability;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

import static java.util.Objects.requireNonNull;

/**
 * This class is for backward compatibility json deserializing and serializing with the V1 json format
 */
@Deprecated
public class ApplicabilityV1 implements Applicability {
  private final Scope scope;
  private final String nodeName;
  private final Integer stripeId;

  public ApplicabilityV1(Scope level, Integer stripeId, String nodeName) {
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
        && ((Applicability) o).getLevel() == Scope.CLUSTER
        && getLevel() == Scope.CLUSTER) {
      return true;
    }
    if (!(o instanceof ApplicabilityV1)) return false;
    ApplicabilityV1 that = (ApplicabilityV1) o;
    return getLevel() == that.getLevel() &&
        Objects.equals(getNodeName(), that.getNodeName()) &&
        Objects.equals(getStripeId(), that.getStripeId());
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
