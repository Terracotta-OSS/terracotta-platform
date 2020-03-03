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

import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * @author Mathieu Carbou
 */
public abstract class TopologyNomadChange extends FilteredNomadChange {

  private final Cluster cluster;

  protected TopologyNomadChange(Cluster cluster, Applicability applicability) {
    super(applicability);
    this.cluster = requireNonNull(cluster);
  }

  public final Cluster getCluster() {
    return cluster;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof TopologyNomadChange)) return false;
    if (!super.equals(o)) return false;
    TopologyNomadChange that = (TopologyNomadChange) o;
    return getCluster().equals(that.getCluster());
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), getCluster());
  }
}
