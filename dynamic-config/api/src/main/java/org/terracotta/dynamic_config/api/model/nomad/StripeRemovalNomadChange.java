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

public class StripeRemovalNomadChange extends StripeNomadChange {

  public StripeRemovalNomadChange(Cluster cluster, Stripe stripe) {
    super(cluster, stripe);
  }

  @Override
  public Cluster apply(Cluster original) {
    requireNonNull(original);

    if (!original.getStripes().contains(getStripe())) {
      throw new IllegalArgumentException("Stripe : " + getStripe() + " is not in cluster: " + original);
    }

    Cluster updated = original.clone();
    updated.removeStripe(getStripe().getUID());
    return updated;
  }

  @Override
  public boolean canUpdateRuntimeTopology(NodeContext nodeContext) {
    return true;
  }

  @Override
  public String getSummary() {
    return "Detaching stripe: " + getStripe().getName() + " from cluster: " + getCluster().getName();
  }

  @Override
  public String toString() {
    return "StripeRemovalChange{" +
        "stripe=" + getStripe().toShapeString() +
        ", cluster=" + getCluster().toShapeString() +
        '}';
  }
}
