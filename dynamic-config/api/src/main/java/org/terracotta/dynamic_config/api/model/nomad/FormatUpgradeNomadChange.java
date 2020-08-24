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
import org.terracotta.dynamic_config.api.model.Version;
import org.terracotta.dynamic_config.api.service.ClusterValidator;

import java.util.List;
import java.util.Random;

import static java.util.Objects.requireNonNull;

/**
 * Nomad change that supports upgrading from a config format to another one
 *
 * @author Mathieu Carbou
 */
public class FormatUpgradeNomadChange extends FilteredNomadChange {

  private final Version from;
  private final Version to;

  public FormatUpgradeNomadChange(Version from, Version to) {
    super(Applicability.cluster());
    this.from = from;
    this.to = to;
  }

  @Override
  public String getSummary() {
    return "Upgrading configuration format from version " + from + " to version " + to;
  }

  @Override
  public Cluster apply(Cluster original) {
    requireNonNull(original);
    Cluster upgraded = original.clone();

    if(from == Version.V1) {
      // From V1 to V2, added required settings are: UIDs, stripe name
      // this migration process happens independently for each node and
      // has to output the exact same result for all the nodes

      // We need to generate the UIDs.
      // The UIDs need to be generated the same way for all the nodes on the same cluster.
      // The "upgrade" process is happening per node, and we have to generate some UIDs
      // that will lead to the results regardless where we are
      // We will then use the cluster name as a seed for the random number generator
      String clusterName = upgraded.getName();
      requireNonNull(clusterName);

      Random random = new Random(clusterName.hashCode());
      upgraded.setUID(upgraded.newUID(random));
      upgraded.getStripes().forEach(stripe -> {
        stripe.setUID(upgraded.newUID(random));
        stripe.getNodes().forEach(node -> {
          node.setUID(upgraded.newUID(random));
        });
      });

      // for stripe names, we will migrate the names has M&M was used to see them
      List<Stripe> stripes = upgraded.getStripes();
      for (int i = 0; i < stripes.size(); i++) {
        if (stripes.get(i).getName() == null) {
          stripes.get(i).setName("stripe[" + i + "]");
        }
      }
    }

    new ClusterValidator(upgraded).validate();

    return upgraded;
  }

  @Override
  public boolean canApplyAtRuntime(NodeContext currentNode) {
    return true;
  }

  public Version getFrom() {
    return from;
  }

  public Version getTo() {
    return to;
  }

  @Override
  public String toString() {
    return "FormatUpgradeNomadChange{" +
        "from=" + from +
        ", to=" + to +
        '}';
  }
}
