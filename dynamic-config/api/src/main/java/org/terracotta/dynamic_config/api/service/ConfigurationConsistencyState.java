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

/**
 * @author Mathieu Carbou
 */
public enum ConfigurationConsistencyState {
  /**
   * All nodes are online and all online nodes are accepting new changes.
   */
  ALL_ACCEPTING,

  /**
   * Some nodes are online (not all) and all online nodes are accepting new changes.
   * Because some nodes are down, we do not know if some offline nodes have some more
   * changes in their append.log.
   */
  ONLINE_ACCEPTING,

  /**
   * All nodes are online and all online nodes have prepared a new change.
   * <p>
   * This situation requires a commit to be replayed, or a rollback to be forced.
   */
  ALL_PREPARED,

  /**
   * Some nodes are online (not all) and all online nodes have prepared a new change.
   * Because some nodes are down, we do not know if some offline nodes have some more
   * changes in their append.log.
   * <p>
   * This situation requires a commit or a rollback to be forced (only the user knows).
   */
  ONLINE_PREPARED,

  /**
   * All nodes are online, and they are all in diagnostic mode, being configured or being repaired.
   */
  ALL_UNINITIALIZED,

  /**
   * Some nodes are online (not all) and those online are all in diagnostic mode, being configured or being repaired.
   */
  ONLINE_UNINITIALIZED,

  /**
   * A change has been prepared on some nodes, but other nodes didn't get it and are
   * ending with another change. This can happen if a Nomad transaction is ended during
   * its prepare phase when the client asks the nodes to prepare themselves.
   * <p>
   * This situation requires a rollback to be replayed.
   */
  PARTIALLY_PREPARED,

  /**
   * A change has been prepared, then rolled back, but the rollback process didn't complete on all online nodes.
   * <p>
   * This situation requires a rollback to be replayed.
   */
  PARTIALLY_ROLLED_BACK,

  /**
   * A change has been prepared, then committed, but the commit process didn't complete on all online nodes.
   * <p>
   * This situation requires a commit to be replayed.
   */
  PARTIALLY_COMMITTED,

  /**
   * The discovery process has failed
   * <p>
   * This situation requires to retry the command.
   */
  DISCOVERY_FAILURE,

  /**
   * The discovery process has failed because another client is currently doing a mutative operation.
   * <p>
   * This situation requires to retry the command.
   */
  CHANGE_IN_PROGRESS,

  /**
   * We have found some change UUIDs that are committed on some servers and rolled back on some others.
   * <p>
   * This situation requires a manual intervention, eventually by resetting the node and re-sync it after a restricted activation.
   */
  INCONSISTENT,

  /**
   * We have found some nodes ending with a different change UUID leading to different configuration results.
   * Some nodes are running with a configuration, some nodes with another one, etc.
   * <p>
   * This situation requires a manual intervention, eventually by resetting the node and re-sync it after a restricted activation.
   */
  PARTITIONED,

  /**
   * Unable to determine the configuration state of the cluster
   */
  UNKNOWN
}
