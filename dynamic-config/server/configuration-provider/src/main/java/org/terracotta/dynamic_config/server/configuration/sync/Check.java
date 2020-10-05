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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.nomad.TopologyNomadChange;
import org.terracotta.dynamic_config.api.service.NomadChangeInfo;
import org.terracotta.nomad.server.ChangeRequestState;

import java.util.Collection;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.Supplier;

import static java.lang.Math.min;
import static org.terracotta.nomad.server.ChangeRequestState.COMMITTED;

/**
 * @author Mathieu Carbou
 */
class Check {

  private static final Logger LOGGER = LoggerFactory.getLogger(Check.class);

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

  static OptionalInt findLastSyncPosition(List<NomadChangeInfo> sourceNomadChanges, Cluster sourceTopology, Cluster currentCluster) {
    // index until which we need to force a sync
    // this will try to find a nomad topology change in the active node that matches the topology used to start the passive node
    // this will cover the case where we need to repair a broken node attachment for example
    int pos = lastIndexOfSameCommittedSourceTopologyChange(sourceNomadChanges, currentCluster);

    // if not found:
    // - either there has been some subsequent changes in the active node after the topology change, and the passive node was activated
    // with the resulting exported topology from active (in this case there is no matching nomad topology change for the passive topology)
    // - either the passive node has been started with a topology not matching at all the active node, and we must fail
    if (pos == -1) {
      if (topologyMatches(sourceTopology, currentCluster)) {
        // if we have found that the active node last change result matches this passive topology, we are fine and we need to force sync of all the append log entries
        pos = sourceNomadChanges.size() - 1;

      } else {
        return OptionalInt.empty();
      }
    }

    LOGGER.trace("findLastSyncPosition({}, {}): {}", sourceNomadChanges, currentCluster, pos);
    return OptionalInt.of(pos);
  }

  static void requireEquals(List<NomadChangeInfo> nomadChanges, List<NomadChangeInfo> sourceNomadChanges, int from, int count) {
    int to = min(min(from + count, nomadChanges.size()), sourceNomadChanges.size());
    LOGGER.trace("requireEquals({}, {}, {}, {}, {})", nomadChanges, sourceNomadChanges, count, from, to);
    for (; from < to; from++) {
      NomadChangeInfo nomadChange = nomadChanges.get(from);
      NomadChangeInfo sourceChange = sourceNomadChanges.get(from);
      if (!nomadChange.matches(sourceChange)) {
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
    final boolean b = nomadChanges.size() == 1;
    LOGGER.trace("isNodeNew({}): {}", nomadChanges, b);
    return b;
  }

  static boolean isJointActivation(List<NomadChangeInfo> nomadChanges, List<NomadChangeInfo> sourceNomadChanges) {
    final boolean b = nomadChanges.get(0).getChangeUuid().equals(sourceNomadChanges.get(0).getChangeUuid());
    LOGGER.trace("isJointActivation({}, {}): {}", nomadChanges, sourceNomadChanges, b);
    return b;
  }

  private static boolean topologyMatches(Cluster sourceTopology, Cluster currentCluster) {
    final boolean b = currentCluster.equals(sourceTopology);
    LOGGER.trace("topologyMatches({}, {}): {}", sourceTopology, currentCluster, b);
    return b;
  }

  private static int lastIndexOfSameCommittedSourceTopologyChange(List<NomadChangeInfo> sourceNomadChanges, Cluster currentCluster) {
    // lookup source changes (reverse order) to find the latest change in force that contains the current topology
    for (int i = sourceNomadChanges.size() - 1; i >= 0; i--) {
      NomadChangeInfo changeInfo = sourceNomadChanges.get(i);
      if (changeInfo.getChangeRequestState() == COMMITTED
          && changeInfo.getNomadChange() instanceof TopologyNomadChange
          && ((TopologyNomadChange) changeInfo.getNomadChange()).getCluster().equals(currentCluster)) {
        // we have found the last topology change in the source node matching the topology used to activate the current node
        LOGGER.trace("lastIndexOfSameCommittedSourceTopologyChange({}, {}): {}", sourceNomadChanges, currentCluster, i);
        return i;
      }
    }
    LOGGER.trace("lastIndexOfSameCommittedSourceTopologyChange({}, {}): {}", sourceNomadChanges, currentCluster, -1);
    return -1;
  }
}
