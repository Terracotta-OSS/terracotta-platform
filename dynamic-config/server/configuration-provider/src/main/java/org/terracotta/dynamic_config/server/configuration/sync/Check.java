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
package org.terracotta.dynamic_config.server.configuration.sync;

import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.nomad.TopologyNomadChange;
import org.terracotta.nomad.server.ChangeRequestState;
import org.terracotta.nomad.server.NomadChangeInfo;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import static java.lang.Math.min;
import static org.terracotta.nomad.server.ChangeRequestState.COMMITTED;

/**
 * @author Mathieu Carbou
 */
class Check {

  static void assertNonEmpty(Collection<?>... collections) {
    for (Collection<?> collection : collections) {
      if (collection.isEmpty()) {
        throw new AssertionError();
      }
    }
  }

  static void assertThat(Supplier<Boolean> check) {
    if (!check.get()) {
      throw new AssertionError();
    }
  }

  static int lastIndexOfSameCommittedSourceTopologyChange(List<NomadChangeInfo> sourceNomadChanges, Cluster currentCluster) {
    // lookup source changes (reverse order) to find the latest change in force that contains the current topology
    for (int i = sourceNomadChanges.size() - 1; i >= 0; i--) {
      NomadChangeInfo changeInfo = sourceNomadChanges.get(i);
      if (changeInfo.getChangeRequestState() == COMMITTED
          && changeInfo.getNomadChange() instanceof TopologyNomadChange
          && ((TopologyNomadChange) changeInfo.getNomadChange()).getCluster().equals(currentCluster)) {
        // we have found the last topology change in the source node matching the topology used to activate the current node
        return i;
      }
    }
    return -1;
  }

  static void requireEquals(List<NomadChangeInfo> nomadChanges, List<NomadChangeInfo> sourceNomadChanges, int from, int count) {
    int to = min(min(from + count, nomadChanges.size()), sourceNomadChanges.size());
    for (; from < to; from++) {
      NomadChangeInfo nomadChange = nomadChanges.get(from);
      NomadChangeInfo sourceChange = sourceNomadChanges.get(from);
      if (!nomadChange.equals(sourceChange)) {
        throw new IllegalStateException("Node cannot sync because the configuration change history does not match: no match on source node for this change on the node:" + nomadChange);
      }
    }
  }

  static boolean canRepair(List<NomadChangeInfo> nomadChanges, List<NomadChangeInfo> sourceNomadChanges) {
    int last = nomadChanges.size() - 1;
    NomadChangeInfo lastNomadChange = nomadChanges.get(last);
    NomadChangeInfo sourceChange = sourceNomadChanges.get(last);
    return lastNomadChange.getChangeUuid().equals(sourceChange.getChangeUuid())
        && lastNomadChange.getChangeRequestState() == ChangeRequestState.PREPARED
        && sourceChange.getChangeRequestState() != ChangeRequestState.PREPARED;
  }

  static boolean isNodeNew(List<NomadChangeInfo> nomadChanges) {
    return nomadChanges.size() == 1;
  }

  static boolean isJointActivation(List<NomadChangeInfo> nomadChanges, List<NomadChangeInfo> sourceNomadChanges) {
    return nomadChanges.get(0).equals(sourceNomadChanges.get(0));
  }
}
