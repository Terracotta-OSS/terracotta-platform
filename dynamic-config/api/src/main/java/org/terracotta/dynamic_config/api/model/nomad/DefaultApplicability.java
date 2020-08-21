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

import static java.util.Objects.requireNonNull;

public class DefaultApplicability implements Applicability {
  private final Scope level;
  private final UID nodeUID;
  private final UID stripeUID;

  public DefaultApplicability(Scope level, UID stripeUID, UID nodeUID) {
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
        && ((Applicability) o).getLevel() == Scope.CLUSTER
        && getLevel() == Scope.CLUSTER) {
      return true;
    }
    if (!(o instanceof DefaultApplicability)) return false;
    DefaultApplicability that = (DefaultApplicability) o;
    return getLevel() == that.getLevel() &&
        Objects.equals(getNodeUID(), that.getNodeUID()) &&
        Objects.equals(getStripeUID(), that.getStripeUID());
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
    if (level != Scope.NODE) {

    }
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
