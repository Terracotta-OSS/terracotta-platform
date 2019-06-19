/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.service.nomad;

import com.terracottatech.nomad.client.Consistency;
import com.terracottatech.nomad.client.change.ChangeResultReceiver;
import com.terracottatech.nomad.messages.DiscoverResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.UUID;

public class LoggingChangeResultReceiver implements ChangeResultReceiver {
  private static final Logger LOGGER = LoggerFactory.getLogger(LoggingChangeResultReceiver.class);

  @Override
  public void startDiscovery(Set<String> servers) {
    print("Gathering state from servers: " + servers);
  }

  @Override
  public void discovered(String server, DiscoverResponse discovery) {
    print("Received server state for: " + server);
  }

  @Override
  public void discoverFail(String server) {
    printError("No response from server: " + server);
  }

  @Override
  public void discoverAlreadyPrepared(String server, UUID changeUUID, String creationHost, String creationUser) {
    printError("Another change (with UUID " + changeUUID + " is already underway on " + server + ". It was started by " + creationUser + " on " + creationHost);
  }

  @Override
  public void discoverClusterInconsistent(UUID changeUuid, Set<String> committedServers, Set<String> rolledBackServers) {
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
  public void discoverRepeated(String server) {
    print("Received server state for: " + server);
  }

  @Override
  public void discoverOtherClient(String server, String lastMutationHost, String lastMutationUser) {
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
  public void prepared(String server) {
    print("Server: " + server + " is prepared to make the change");
  }

  @Override
  public void prepareFail(String server) {
    printError("Server: " + server + " failed when asked to prepare to make the change");
  }

  @Override
  public void prepareOtherClient(String server, String lastMutationHost, String lastMutationUser) {
    printError("Another process run on " + lastMutationHost + " by " + lastMutationUser + " changed the state on " + server);
  }

  @Override
  public void prepareChangeUnacceptable(String server, String rejectionReason) {
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
  public void committed(String server) {
    print("Server: " + server + " has committed the change");
  }

  @Override
  public void commitFail(String server) {
    printError("Server: " + server + " failed when asked to commit the change");
  }

  @Override
  public void commitOtherClient(String server, String lastMutationHost, String lastMutationUser) {
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
  public void rolledBack(String server) {
    print("Server: " + server + " has rolled back the change");
  }

  @Override
  public void rollbackFail(String server) {
    printError("Server: " + server + " failed when asked to roll back the change");
  }

  @Override
  public void rollbackOtherClient(String server, String lastMutationHost, String lastMutationUser) {
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
      case MAY_NEED_RECOVERY:
        printError("The recovery process may need to be run");
        break;
      case UNKNOWN_BUT_NO_CHANGE:
        printError("Could not fully gather the state of the servers");
        break;
      case UNRECOVERABLY_INCONSISTENT:
        throw new AssertionError("Please seek support. The cluster is inconsistent and cannot be trivially recovered.");
      default:
        throw new AssertionError("Unknown Consistency: " + consistency);
    }
  }

  private void printError(String line) {
    LOGGER.error(line);
  }

  private void print(String line) {
    LOGGER.info(line);
  }
}
