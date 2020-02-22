/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.server.sync;

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

  static int lastIndexOfSameCommittedActiveTopologyChange(List<NomadChangeInfo> activeNomadChanges, Cluster passiveCluster) {
    // lookup active changes (reverse order) to find the latest change in force that contains the passive topology
    for (int i = activeNomadChanges.size() - 1; i >= 0; i--) {
      NomadChangeInfo changeInfo = activeNomadChanges.get(i);
      if (changeInfo.getChangeRequestState() == COMMITTED
          && changeInfo.getNomadChange() instanceof TopologyNomadChange
          && ((TopologyNomadChange) changeInfo.getNomadChange()).getCluster().equals(passiveCluster)) {
        // we have found the last topology change in the active node matching the topology used to activate the passive node
        return i;
      }
    }
    return -1;
  }

  static void requireEquals(List<NomadChangeInfo> passiveNomadChanges, List<NomadChangeInfo> activeNomadChanges, int from, int count) {
    int to = min(min(from + count, passiveNomadChanges.size()), activeNomadChanges.size());
    for (; from < to; from++) {
      NomadChangeInfo passiveChange = passiveNomadChanges.get(from);
      NomadChangeInfo activeChange = activeNomadChanges.get(from);
      if (!passiveChange.equals(activeChange)) {
        throw new IllegalStateException("Passive cannot sync because the configuration change history does not match: no match on active for this change on passive:" + passiveChange);
      }
    }
  }

  static boolean canRepair(List<NomadChangeInfo> passiveNomadChanges, List<NomadChangeInfo> activeNomadChanges) {
    int last = passiveNomadChanges.size() - 1;
    NomadChangeInfo lastPassiveChange = passiveNomadChanges.get(last);
    NomadChangeInfo activeChange = activeNomadChanges.get(last);
    return lastPassiveChange.getChangeUuid().equals(activeChange.getChangeUuid())
        && lastPassiveChange.getChangeRequestState() == ChangeRequestState.PREPARED
        && activeChange.getChangeRequestState() != ChangeRequestState.PREPARED;
  }

  static boolean isPassiveMew(List<NomadChangeInfo> passiveNomadChanges) {
    return passiveNomadChanges.size() == 1;
  }

  static boolean isJointActivation(List<NomadChangeInfo> passiveNomadChanges, List<NomadChangeInfo> activeNomadChanges) {
    return passiveNomadChanges.get(0).equals(activeNomadChanges.get(0));
  }
}
