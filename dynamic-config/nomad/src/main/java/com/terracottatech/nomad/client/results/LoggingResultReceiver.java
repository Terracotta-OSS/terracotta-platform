/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.client.results;

import com.terracottatech.nomad.client.Consistency;
import com.terracottatech.nomad.client.change.ChangeResultReceiver;
import com.terracottatech.nomad.client.recovery.RecoveryResultReceiver;
import com.terracottatech.nomad.messages.DiscoverResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.UUID;

public class LoggingResultReceiver<T> implements ChangeResultReceiver<T>, RecoveryResultReceiver<T> {
  private static final Logger LOGGER = LoggerFactory.getLogger(LoggingResultReceiver.class);

  @Override
  public void startTakeover() {
    trace("Start takeover");
  }

  @Override
  public void takeover(InetSocketAddress node) {
    trace("Takeover: " + node);
  }

  @Override
  public void takeoverOtherClient(InetSocketAddress node, String lastMutationHost, String lastMutationUser) {
    error("Takeover of other client: node=" + node + ", lastMutationHost=" + lastMutationHost + ", lastMutationUser=" + lastMutationUser);
  }

  @Override
  public void takeoverFail(InetSocketAddress node, String reason) {
    error("Takeover has failed: " + node + ": " + reason);
  }

  @Override
  public void endTakeover() {
    trace("End takeover");
  }

  @Override
  public void startDiscovery(Collection<InetSocketAddress> servers) {
    trace("Gathering state from servers: " + servers);
  }

  @Override
  public void discovered(InetSocketAddress node, DiscoverResponse<T> discovery) {
    trace("Received node state for: " + node);
  }

  @Override
  public void discoverFail(InetSocketAddress node, String reason) {
    error("Discover failed on node: " + node + ". Reason: " + reason);
  }

  @Override
  public void discoverAlreadyPrepared(InetSocketAddress node, UUID changeUUID, String creationHost, String creationUser) {
    error("Another change (with UUID " + changeUUID + " is already underway on " + node + ". It was started by " + creationUser + " on " + creationHost);
  }

  @Override
  public void discoverClusterInconsistent(UUID changeUuid, Collection<InetSocketAddress> committedServers, Collection<InetSocketAddress> rolledBackServers) {
    error("UNRECOVERABLE: Inconsistent cluster for change: " + changeUuid + ". Committed on: " + committedServers + "; rolled back on: " + rolledBackServers);
  }

  @Override
  public void endDiscovery() {
    trace("Finished first round of gathering state");
  }

  @Override
  public void startSecondDiscovery() {
    trace("Starting second round of gathering state");
  }

  @Override
  public void discoverRepeated(InetSocketAddress node) {
    trace("Received node state for: " + node);
  }

  @Override
  public void discoverOtherClient(InetSocketAddress node, String lastMutationHost, String lastMutationUser) {
    error("Another process running on " + lastMutationHost + " by " + lastMutationUser + " changed the state on " + node);
  }

  @Override
  public void endSecondDiscovery() {
    trace("Finished second round of gathering state");
  }

  @Override
  public void startPrepare(UUID newChangeUuid) {
    trace("No node is currently making a change. Starting a new change with UUID: " + newChangeUuid);
  }

  @Override
  public void prepared(InetSocketAddress node) {
    trace("Node: " + node + " is prepared to make the change");
  }

  @Override
  public void prepareFail(InetSocketAddress node, String reason) {
    error("Node: " + node + " failed when asked to prepare to make the change. Reason: " + reason);
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
    trace("Finished asking servers to prepare to make the change");
  }

  @Override
  public void startCommit() {
    trace("Committing the change");
  }

  @Override
  public void committed(InetSocketAddress node) {
    trace("Node: " + node + " has committed the change");
  }

  @Override
  public void commitFail(InetSocketAddress node, String reason) {
    error("Commit failed for node " + node + ". Reason: " + reason);
  }

  @Override
  public void commitOtherClient(InetSocketAddress node, String lastMutationHost, String lastMutationUser) {
    error("Another process running on " + lastMutationHost + " by " + lastMutationUser + " changed the state on " + node);
  }

  @Override
  public void endCommit() {
    trace("Finished asking servers to commit the change");
  }

  @Override
  public void startRollback() {
    trace("Rolling back the change");
  }

  @Override
  public void rolledBack(InetSocketAddress node) {
    trace("Node: " + node + " has rolled back the change");
  }

  @Override
  public void rollbackFail(InetSocketAddress node, String reason) {
    error("Rollback failed for node: " + node + ". Reason: " + reason);
  }

  @Override
  public void rollbackOtherClient(InetSocketAddress node, String lastMutationHost, String lastMutationUser) {
    error("Another process running on " + lastMutationHost + " by " + lastMutationUser + " changed the state on " + node);
  }

  @Override
  public void endRollback() {
    trace("Finished asking servers to rollback the change");
  }

  @Override
  public void done(Consistency consistency) {
    switch (consistency) {
      case CONSISTENT:
        trace("The change has been made successfully");
        break;
      case MAY_NEED_FORCE_RECOVERY:
        error("Please run the 'diagnostic' command to diagnose the configuration state and try to run the 'repair' command and force either a commit or rollback.");
        break;
      case MAY_NEED_RECOVERY:
      case UNKNOWN_BUT_NO_CHANGE:
        error("Please run the 'diagnostic' command to diagnose the configuration state and try to run the 'repair' command.");
        break;
      case UNRECOVERABLY_INCONSISTENT:
        error("Please run the 'diagnostic' command to diagnose the configuration state and please seek support. The cluster is inconsistent and cannot be trivially recovered.");
        break;
      default:
        throw new AssertionError("Unknown Consistency: " + consistency);
    }
  }

  protected void error(String line) {
    // debug level is normal hereL this class is used to "trace" all Nomad callbacks and print them to the console
    // if we are in verbose mode (trace level)
    // the errors occurring in nomad are handled at another place
    LOGGER.debug(line);
  }

  protected void trace(String line) {
    LOGGER.debug(line);
  }
}
