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
    print("Start takeover");
  }

  @Override
  public void takeover(InetSocketAddress server) {
    print("Takeover: " + server);
  }

  @Override
  public void takeoverOtherClient(InetSocketAddress server, String lastMutationHost, String lastMutationUser) {
    printError("Takeover of other client: server=" + server + ", lastMutationHost=" + lastMutationHost + ", lastMutationUser=" + lastMutationUser);
  }

  @Override
  public void takeoverFail(InetSocketAddress server, String reason) {
    printError("Takeover has failed: " + server + ": " + reason);
  }

  @Override
  public void endTakeover() {
    print("End takeover");
  }

  @Override
  public void startDiscovery(Collection<InetSocketAddress> servers) {
    print("Gathering state from servers: " + servers);
  }

  @Override
  public void discovered(InetSocketAddress server, DiscoverResponse<T> discovery) {
    print("Received server state for: " + server);
  }

  @Override
  public void discoverFail(InetSocketAddress server, String reason) {
    printError("No response from server: " + server + ". Reason: " + reason);
  }

  @Override
  public void discoverAlreadyPrepared(InetSocketAddress server, UUID changeUUID, String creationHost, String creationUser) {
    printError("Another change (with UUID " + changeUUID + " is already underway on " + server + ". It was started by " + creationUser + " on " + creationHost);
  }

  @Override
  public void discoverClusterInconsistent(UUID changeUuid, Collection<InetSocketAddress> committedServers, Collection<InetSocketAddress> rolledBackServers) {
    printError("UNRECOVERABLE: Inconsistent cluster for change: " + changeUuid + ". Committed on: " + committedServers + "; rolled back on: " + rolledBackServers);
  }

  @Override
  public void endDiscovery() {
    print("Finished first round of gathering state");
  }

  @Override
  public void startSecondDiscovery() {
    print("Starting second round of gathering state");
  }

  @Override
  public void discoverRepeated(InetSocketAddress server) {
    print("Received server state for: " + server);
  }

  @Override
  public void discoverOtherClient(InetSocketAddress server, String lastMutationHost, String lastMutationUser) {
    printError("Another process run on " + lastMutationHost + " by " + lastMutationUser + " changed the state on " + server);
  }

  @Override
  public void endSecondDiscovery() {
    print("Finished second round of gathering state");
  }

  @Override
  public void startPrepare(UUID newChangeUuid) {
    print("No server is currently making a change. Starting a new change with UUID: " + newChangeUuid);
  }

  @Override
  public void prepared(InetSocketAddress server) {
    print("Server: " + server + " is prepared to make the change");
  }

  @Override
  public void prepareFail(InetSocketAddress server, String reason) {
    printError("Server: " + server + " failed when asked to prepare to make the change. Reason: " + reason);
  }

  @Override
  public void prepareOtherClient(InetSocketAddress server, String lastMutationHost, String lastMutationUser) {
    printError("Another process run on " + lastMutationHost + " by " + lastMutationUser + " changed the state on " + server);
  }

  @Override
  public void prepareChangeUnacceptable(InetSocketAddress server, String rejectionReason) {
    printError("Server: " + server + " rejected the change as unacceptable because: " + rejectionReason);
  }

  @Override
  public void endPrepare() {
    print("Finished asking servers to prepare to make the change");
  }

  @Override
  public void startCommit() {
    print("Committing the change");
  }

  @Override
  public void committed(InetSocketAddress server) {
    print("Server: " + server + " has committed the change");
  }

  @Override
  public void commitFail(InetSocketAddress server, String reason) {
    printError("Server: " + server + " failed when asked to commit the change: " + reason);
  }

  @Override
  public void commitOtherClient(InetSocketAddress server, String lastMutationHost, String lastMutationUser) {
    printError("Another process run on " + lastMutationHost + " by " + lastMutationUser + " changed the state on " + server);
  }

  @Override
  public void endCommit() {
    print("Finished asking servers to commit the change");
  }

  @Override
  public void startRollback() {
    print("Rolling back the change");
  }

  @Override
  public void rolledBack(InetSocketAddress server) {
    print("Server: " + server + " has rolled back the change");
  }

  @Override
  public void rollbackFail(InetSocketAddress server, String reason) {
    printError("Server: " + server + " failed when asked to roll back the change. Reason: " + reason);
  }

  @Override
  public void rollbackOtherClient(InetSocketAddress server, String lastMutationHost, String lastMutationUser) {
    printError("Another process run on " + lastMutationHost + " by " + lastMutationUser + " changed the state on " + server);
  }

  @Override
  public void endRollback() {
    print("Finished asking servers to rollback the change");
  }

  @Override
  public void done(Consistency consistency) {
    switch (consistency) {
      case CONSISTENT:
        print("The change has been made successfully");
        break;
      case MAY_NEED_FORCE_RECOVERY:
        printError("Please run the 'repair' command again and force either a commit or rollback");
        break;
      case MAY_NEED_RECOVERY:
      case UNKNOWN_BUT_NO_CHANGE:
        printError("Please run the 'diagnostic' command to diagnose the configuration state and try to run the 'repair' command.");
        break;
      case UNRECOVERABLY_INCONSISTENT:
        printError("Please run the 'diagnostic' command to diagnose the configuration state and please seek support. The cluster is inconsistent and cannot be trivially recovered.");
        break;
      default:
        throw new AssertionError("Unknown Consistency: " + consistency);
    }
  }

  private void printError(String line) {
    // debug level is normal hereL this class is used to "trace" all Nomad callbacks and print them to the console
    // if we are in verbose mode (trace level)
    // the errors occurring in nomad are handled at another place
    LOGGER.debug(line);
  }

  private void print(String line) {
    LOGGER.debug(line);
  }
}
