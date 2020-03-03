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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.terracotta.dynamic_config.api.model.Cluster;

import static java.util.Objects.requireNonNull;

/**
 * Nomad change that supports the initial setup of the config repository
 *
 * @author Mathieu Carbou
 */
@JsonTypeName("ClusterActivationNomadChange")
public class ClusterActivationNomadChange extends TopologyNomadChange {

  @JsonCreator
  public ClusterActivationNomadChange(@JsonProperty(value = "cluster", required = true) Cluster cluster) {
    super(cluster, Applicability.cluster());
    requireNonNull(cluster.getName());
  }

  @Override
  public String getSummary() {
    return "Activating cluster";
  }

  @Override
  public Cluster apply(Cluster original) {
    return getCluster();
  }

  @Override
  public boolean canApplyAtRuntime() {
    return false;
  }

  @Override
  public String toString() {
    return "ClusterActivationNomadChange{" +
        "cluster=" + getCluster() +
        ", applicability=" + getApplicability() +
        '}';
  }
}
