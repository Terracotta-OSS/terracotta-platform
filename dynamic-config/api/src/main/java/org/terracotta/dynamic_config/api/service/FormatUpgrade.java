/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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
package org.terracotta.dynamic_config.api.service;

import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.ClusterState;
import org.terracotta.dynamic_config.api.model.Version;

import java.util.Random;

import static java.util.Objects.requireNonNull;

/**
 * @author Mathieu Carbou
 */
public class FormatUpgrade {

  public Cluster upgrade(Cluster original, Version from) {
    requireNonNull(original);
    requireNonNull(from);

    Cluster upgraded = original.clone();

    if (from == Version.V1) {
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
        stripe.getNodes().forEach(node -> node.setUID(upgraded.newUID(random)));
      });

      // Generate only stripe names when migrating from V1 to V2.
      // Existing node names should not be touched
      NameGenerator.assignFriendlyStripeNames(upgraded, new Random(clusterName.hashCode()));
    }

    new ClusterValidator(upgraded).validate(ClusterState.CONFIGURING);

    return upgraded;
  }
}
