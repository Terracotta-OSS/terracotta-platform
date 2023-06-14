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
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.Stripe;

import static java.util.Objects.requireNonNull;

public class StripeAdditionNomadChange extends StripeNomadChange {

  // For json
  private StripeAdditionNomadChange() {}

  public StripeAdditionNomadChange(Cluster cluster, Stripe stripe) {
    super(cluster, stripe);

    if (cluster.getStripes().stream().noneMatch(stripe::equals)) {
      throw new IllegalArgumentException("Stripe " + stripe.toShapeString() + " is not part of cluster " + cluster.toShapeString());
    }
  }

  @Override
  public Cluster apply(Cluster original) {
    requireNonNull(original);

    if (original.getStripes().contains(getStripe())) {
      throw new IllegalArgumentException("Stripe :" + getStripe() + " already exists in cluster: " + original);
    }

    Cluster updated = original.clone();
    updated.addStripe(getStripe().clone());
    return updated;
  }

  @Override
  public boolean canUpdateRuntimeTopology(NodeContext nodeContext) {
    return true;
  }

  @Override
  public String getSummary() {
    return "Attaching stripe: " + getStripe().toShapeString() + " to cluster: " + getCluster().getName();
  }

  @Override
  public String toString() {
    return "StripeAdditionChange{" +
        "stripe=" + getStripe().toShapeString() +
        ", cluster=" + getCluster().toShapeString() +
        '}';
  }
}
