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
import org.terracotta.dynamic_config.api.model.nomad.LockAwareDynamicConfigNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.TopologyNomadChange;
import org.terracotta.dynamic_config.api.service.NomadChangeInfo;
import org.terracotta.nomad.client.change.NomadChange;

import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.OptionalInt;

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

  static void assertEmpty(Collection<?>... collections) {
    for (Collection<?> collection : collections) {
      if (!collection.isEmpty()) {
        throw new AssertionError(collection);
      }
    }
  }

  static void assertTrue(boolean b) {
    if (!b) {
      throw new AssertionError();
    }
  }

  static OptionalInt findLastSyncPosition(Deque<NomadChangeInfo> sourceNomadChanges, Cluster sourceTopology, Cluster currentCluster) {
    // index until which we need to force a sync
    // this will try to find a nomad topology change in the active node that matches the topology used to start the passive node
    // this will cover the case where we need to repair a broken node attachment for example
    int pos = lastIndexOfSameTopologyCommitted(sourceNomadChanges, currentCluster);

    // if not found:
    // - either there has been some subsequent changes in the active node after the topology change, and the passive node was activated
    // with the resulting exported topology from active (in this case there is no matching nomad topology change for the passive topology)
    // - either the passive node has been started with a topology not matching at all the active node, and we must fail
    if (pos == -1) {
      if (topologyMatches(sourceTopology, currentCluster)) {
        // if we have found that the active node last change result matches this passive topology, we are fine, and we need to force sync of all the append log entries
        pos = sourceNomadChanges.size() - 1;

      } else {
        LOGGER.trace("findLastSyncPosition(): {}", pos);
        return OptionalInt.empty();
      }
    }

    LOGGER.trace("findLastSyncPosition(): {}", pos);
    return OptionalInt.of(pos);
  }

  static boolean isNodeNew(Collection<NomadChangeInfo> nomadChanges) {
    final boolean b = nomadChanges.size() == 1;
    LOGGER.trace("isNodeNew({}): {}", nomadChanges, b);
    return b;
  }

  static boolean isJointActivation(NomadChangeInfo thisFirst, NomadChangeInfo sourceFirst) {
    final boolean b = thisFirst.getChangeUuid().equals(sourceFirst.getChangeUuid())
        && thisFirst.getChangeRequestState() == COMMITTED
        && sourceFirst.getChangeRequestState() == COMMITTED;
    LOGGER.trace("isJointActivation({}, {}): {}", thisFirst, sourceFirst, b);
    return b;
  }

  static NomadChange unwrap(NomadChange change) {
    if (change instanceof LockAwareDynamicConfigNomadChange) {
      return ((LockAwareDynamicConfigNomadChange) change).getChange();
    }
    return change;
  }

  private static boolean topologyMatches(Cluster sourceTopology, Cluster currentCluster) {
    final boolean b = currentCluster.equals(sourceTopology);
    LOGGER.trace("topologyMatches(): {}", b);
    return b;
  }

  private static int lastIndexOfSameTopologyCommitted(Deque<NomadChangeInfo> sourceNomadChanges, Cluster currentCluster) {
    // lookup source changes (reverse order) to find the latest change in force that contains the current topology
    Iterator<NomadChangeInfo> reverseIterator = sourceNomadChanges.descendingIterator();
    for (int i = sourceNomadChanges.size() - 1; reverseIterator.hasNext() && i >= 0; i--) {
      NomadChangeInfo changeInfo = reverseIterator.next();
      if (changeInfo.getChangeRequestState() == COMMITTED
          && unwrap(changeInfo.getNomadChange()) instanceof TopologyNomadChange
          && ((TopologyNomadChange) unwrap(changeInfo.getNomadChange())).getCluster().equals(currentCluster)) {
        // we have found the last topology change in the source node matching the topology used to activate the current node
        LOGGER.trace("lastIndexOfSameTopologyCommitted(): {}", i);
        return i;
      }
    }
    LOGGER.trace("lastIndexOfSameTopologyCommitted(): {}", -1);
    return -1;
  }
}
