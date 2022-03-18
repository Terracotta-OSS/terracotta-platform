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
package org.terracotta.nomad.client.results;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.nomad.client.Consistency;
import org.terracotta.nomad.client.change.ChangeResultReceiver;
import org.terracotta.nomad.client.recovery.RecoveryResultReceiver;
import org.terracotta.nomad.messages.DiscoverResponse;
import org.terracotta.nomad.server.ChangeRequestState;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.UUID;

public class LoggingResultReceiver<T> implements ChangeResultReceiver<T>, RecoveryResultReceiver<T> {
  private static final Logger LOGGER = LoggerFactory.getLogger(LoggingResultReceiver.class);
  private volatile ChangeRequestState state;

  @Override
  public void startTakeover() {
    log("Start takeover");
  }

  @Override
  public void takeover(InetSocketAddress node) {
    log("Takeover: " + node);
  }

  @Override
  public void takeoverOtherClient(InetSocketAddress node, String lastMutationHost, String lastMutationUser) {
    error("Takeover of other client: node=" + node + ", lastMutationHost=" + lastMutationHost + ", lastMutationUser=" + lastMutationUser);
  }

  @Override
  public void takeoverFail(InetSocketAddress node, Throwable reason) {
    error("Takeover has failed: " + node, reason);
  }

  @Override
  public void endTakeover() {
    log("End takeover");
  }

  @Override
  public void startDiscovery(Collection<InetSocketAddress> servers) {
    log("Gathering state from servers: " + servers);
  }

  @Override
  public void discovered(InetSocketAddress node, DiscoverResponse<T> discovery) {
    log("Received node state for: " + node);
  }

  @Override
  public void discoverFail(InetSocketAddress node, Throwable reason) {
    error("Discover failed on node: " + node, reason);
  }

  @Override
  public void discoverAlreadyPrepared(InetSocketAddress node, UUID changeUUID, String creationHost, String creationUser) {
    error("Another change (with UUID " + changeUUID + " is already underway on " + node + ". It was started by " + creationUser + " on " + creationHost);
  }

  @Override
  public void discoverConfigInconsistent(UUID changeUuid, Collection<InetSocketAddress> committedServers, Collection<InetSocketAddress> rolledBackServers) {
    error("UNRECOVERABLE: Inconsistent config for change: " + changeUuid + ". Committed on: " + committedServers + "; rolled back on: " + rolledBackServers);
  }

  @Override
  public void discoverConfigPartitioned(Collection<Collection<InetSocketAddress>> partitions) {
    error("UNRECOVERABLE: Partitioned configuration on cluster. Subsets: " + partitions);
  }

  @Override
  public void endDiscovery() {
    log("Finished first round of gathering state");
  }

  @Override
  public void startSecondDiscovery() {
    log("Starting second round of gathering state");
  }

  @Override
  public void discoverRepeated(InetSocketAddress node) {
    log("Received node state for: " + node);
  }

  @Override
  public void discoverOtherClient(InetSocketAddress node, String lastMutationHost, String lastMutationUser) {
    error("Another process running on " + lastMutationHost + " by " + lastMutationUser + " changed the state on " + node);
  }

  @Override
  public void endSecondDiscovery() {
    log("Finished second round of gathering state");
  }

  @Override
  public void startPrepare(UUID newChangeUuid) {
    log("No node is currently making a change. Starting a new change with UUID: " + newChangeUuid);
  }

  @Override
  public void prepared(InetSocketAddress node) {
    log("Node: " + node + " is prepared to make the change");
  }

  @Override
  public void prepareFail(InetSocketAddress node, Throwable reason) {
    error("Node: " + node + " failed when asked to prepare to make the change", reason);
  }

  @Override
  public void prepareOtherClient(InetSocketAddress node, String lastMutationHost, String lastMutationUser) {
    error("Another process running on " + lastMutationHost + " by " + lastMutationUser + " changed the state on " + node);
  }

  @Override
  public void prepareChangeUnacceptable(InetSocketAddress node, String rejectionReason) {
    error("Prepare rejected for node " + node + ". Reason: " + rejectionReason);
  }

  @Override
  public void endPrepare() {
    state = ChangeRequestState.PREPARED;
    log("Finished asking servers to prepare to make the change");
  }

  @Override
  public void startCommit() {
    log("Committing the change");
  }

  @Override
  public void committed(InetSocketAddress node) {
    log("Node: " + node + " has committed the change");
  }

  @Override
  public void commitFail(InetSocketAddress node, Throwable reason) {
    error("Commit failed for node " + node, reason);
  }

  @Override
  public void commitOtherClient(InetSocketAddress node, String lastMutationHost, String lastMutationUser) {
    error("Another process running on " + lastMutationHost + " by " + lastMutationUser + " changed the state on " + node);
  }

  @Override
  public void endCommit() {
    state = ChangeRequestState.COMMITTED;
    log("Finished asking servers to commit the change");
  }

  @Override
  public void startRollback() {
    log("Rolling back the change");
  }

  @Override
  public void rolledBack(InetSocketAddress node) {
    log("Node: " + node + " has rolled back the change");
  }

  @Override
  public void rollbackFail(InetSocketAddress node, Throwable reason) {
    error("Rollback failed for node: " + node, reason);
  }

  @Override
  public void rollbackOtherClient(InetSocketAddress node, String lastMutationHost, String lastMutationUser) {
    error("Another process running on " + lastMutationHost + " by " + lastMutationUser + " changed the state on " + node);
  }

  @Override
  public void endRollback() {
    state = ChangeRequestState.ROLLED_BACK;
    log("Finished asking servers to rollback the change");
  }

  @Override
  public void done(Consistency consistency) {
    switch (consistency) {
      case CONSISTENT:
        log("The transaction is completed. State: " + state);
        break;
      case MAY_NEED_RECOVERY:
        error("Please run the 'diagnostic' command to diagnose the configuration state and try to run the 'repair' command.");
        break;
      case UNKNOWN_BUT_NO_CHANGE:
        log("Unable to determine new configuration consistency: configuration has not changed as no mutative operation has been performed");
        break;
      case UNRECOVERABLY_INCONSISTENT:
      case UNRECOVERABLY_PARTITIONNED:
        error("Please run the 'diagnostic' command to diagnose the configuration state and please seek support. The cluster is inconsistent or partitioned and cannot be trivially recovered.");
        break;
      default:
        throw new AssertionError("Unknown Consistency: " + consistency);
    }
  }

  @Override
  public void cannotDecideOverCommitOrRollback() {
    error("Please run the 'diagnostic' command to diagnose the configuration state and try to run the 'repair' command. Please refer to the troubleshooting guide for more help.");
  }

  private void error(String line) {
    error(line, null);
  }

  protected void error(String line, Throwable e) {
    // debug level is normal hereL this class is used to "trace" all Nomad callbacks and print them to the console
    // if we are in verbose mode (trace level)
    // the errors occurring in nomad are handled at another place
    if (e == null) {
      LOGGER.debug(line);
    } else {
      LOGGER.debug(line + " Error: " + e.getMessage(), e);
    }
  }

  protected void log(String line) {
    LOGGER.debug(line);
  }
}
