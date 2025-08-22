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
package org.terracotta.dynamic_config.api.model.nomad;

import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Version;

/**
 * Nomad change that supports upgrading from a config format to another one.
 * <p>
 * It also acts as a starting point of an append.log for the sync process.
 *
 * @author Mathieu Carbou
 */
public class FormatUpgradeNomadChange extends ClusterActivationNomadChange {

  private final Version from;
  private final Version to;

  // For Json
  FormatUpgradeNomadChange() {
    from = null;
    to = null;
  }

  public FormatUpgradeNomadChange(Version from, Version to, Cluster cluster) {
    super(cluster);
    this.from = from;
    this.to = to;
  }

  @Override
  public String getSummary() {
    return "Upgrading configuration format from version " + from + " to version " + to;
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
        ", cluster=" + getCluster().toShapeString() +
        '}';
  }
}
