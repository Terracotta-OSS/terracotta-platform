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

import static java.util.Objects.requireNonNull;

/**
 * Nomad change that supports the initial setup of the configuration directory
 *
 * @author Mathieu Carbou
 */
public class ClusterActivationNomadChange extends TopologyNomadChange {

  public ClusterActivationNomadChange(Cluster cluster) {
    super(cluster, Applicability.cluster());
    requireNonNull(cluster.getName());
  }

  @Override
  public String getSummary() {
    return "Activating cluster: " + getCluster().getName();
  }

  @Override
  public String getType() {
    return "ClusterActivationNomadChange";
  }

  @Override
  public Cluster apply(Cluster original) {
    return getCluster();
  }

  @Override
  public boolean canUpdateRuntimeTopology(NodeContext currentNode) {
    return false;
  }

  @Override
  public String toString() {
    return "ClusterActivationNomadChange{" +
        "cluster=" + getCluster().toShapeString() +
        ", applicability=" + getApplicability() +
        '}';
  }
}
