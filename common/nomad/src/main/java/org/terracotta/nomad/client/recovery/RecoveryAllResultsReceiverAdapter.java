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
package org.terracotta.nomad.client.recovery;

import org.terracotta.nomad.client.Consistency;
import org.terracotta.nomad.client.results.AllResultsReceiver;
import org.terracotta.nomad.messages.DiscoverResponse;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public class RecoveryAllResultsReceiverAdapter<T> implements AllResultsReceiver<T> {
  private final RecoveryResultReceiver<T> recoveryResultReceiver;

  public RecoveryAllResultsReceiverAdapter(RecoveryResultReceiver<T> recoveryResultReceiver) {
    this.recoveryResultReceiver = recoveryResultReceiver;
  }

  @Override
  public void startDiscovery(Collection<InetSocketAddress> servers) {
    recoveryResultReceiver.startDiscovery(servers);
  }

  @Override
  public void discovered(InetSocketAddress server, DiscoverResponse<T> discovery) {
    recoveryResultReceiver.discovered(server, discovery);
  }

  @Override
  public void discoverFail(InetSocketAddress server, String reason) {
    recoveryResultReceiver.discoverFail(server, reason);
  }

  @Override
  public void discoverClusterInconsistent(UUID changeUuid, Collection<InetSocketAddress> committedServers, Collection<InetSocketAddress> rolledBackServers) {
    recoveryResultReceiver.discoverClusterInconsistent(changeUuid, committedServers, rolledBackServers);
  }

  @Override
  public void discoverClusterDesynchronized(Map<UUID, Collection<InetSocketAddress>> lastChangeUuids) {
    recoveryResultReceiver.discoverClusterDesynchronized(lastChangeUuids);
  }

  @Override
  public void endDiscovery() {
    recoveryResultReceiver.endDiscovery();
  }

  @Override
  public void startSecondDiscovery() {
    recoveryResultReceiver.startSecondDiscovery();
  }

  @Override
  public void discoverRepeated(InetSocketAddress server) {
    recoveryResultReceiver.discoverRepeated(server);
  }

  @Override
  public void discoverOtherClient(InetSocketAddress server, String lastMutationHost, String lastMutationUser) {
    recoveryResultReceiver.discoverOtherClient(server, lastMutationHost, lastMutationUser);
  }

  @Override
  public void endSecondDiscovery() {
    recoveryResultReceiver.endSecondDiscovery();
  }

  @Override
  public void discoverAlreadyPrepared(InetSocketAddress server, UUID changeUuid, String creationHost, String creationUser) {
    throw new AssertionError("This should not be called during the recovery process");
  }

  @Override
  public void startPrepare(UUID newChangeUuid) {
    throw new AssertionError("This should not be called during the recovery process");
  }

  @Override
  public void prepared(InetSocketAddress server) {
    throw new AssertionError("This should not be called during the recovery process");
  }

  @Override
  public void prepareFail(InetSocketAddress server, String reason) {
    throw new AssertionError("This should not be called during the recovery process");
  }

  @Override
  public void prepareOtherClient(InetSocketAddress server, String lastMutationHost, String lastMutationUser) {
    throw new AssertionError("This should not be called during the recovery process");
  }

  @Override
  public void prepareChangeUnacceptable(InetSocketAddress server, String rejectionReason) {
    throw new AssertionError("This should not be called during the recovery process");
  }

  @Override
  public void endPrepare() {
    throw new AssertionError("This should not be called during the recovery process");
  }

  @Override
  public void startTakeover() {
    recoveryResultReceiver.startTakeover();
  }

  @Override
  public void takeover(InetSocketAddress server) {
    recoveryResultReceiver.takeover(server);
  }

  @Override
  public void takeoverOtherClient(InetSocketAddress server, String lastMutationHost, String lastMutationUser) {
    recoveryResultReceiver.takeoverOtherClient(server, lastMutationHost, lastMutationUser);
  }

  @Override
  public void takeoverFail(InetSocketAddress server, String reason) {
    recoveryResultReceiver.takeoverFail(server, reason);
  }

  @Override
  public void endTakeover() {
    recoveryResultReceiver.endTakeover();
  }

  @Override
  public void startCommit() {
    recoveryResultReceiver.startCommit();
  }

  @Override
  public void committed(InetSocketAddress server) {
    recoveryResultReceiver.committed(server);
  }

  @Override
  public void commitFail(InetSocketAddress server, String reason) {
    recoveryResultReceiver.commitFail(server, reason);
  }

  @Override
  public void commitOtherClient(InetSocketAddress server, String lastMutationHost, String lastMutationUser) {
    recoveryResultReceiver.commitOtherClient(server, lastMutationHost, lastMutationUser);
  }

  @Override
  public void endCommit() {
    recoveryResultReceiver.endCommit();
  }

  @Override
  public void startRollback() {
    recoveryResultReceiver.startRollback();
  }

  @Override
  public void rolledBack(InetSocketAddress server) {
    recoveryResultReceiver.rolledBack(server);
  }

  @Override
  public void rollbackFail(InetSocketAddress server, String reason) {
    recoveryResultReceiver.rollbackFail(server, reason);
  }

  @Override
  public void rollbackOtherClient(InetSocketAddress server, String lastMutationHost, String lastMutationUser) {
    recoveryResultReceiver.rollbackOtherClient(server, lastMutationHost, lastMutationUser);
  }

  @Override
  public void endRollback() {
    recoveryResultReceiver.endRollback();
  }

  @Override
  public void done(Consistency consistency) {
    recoveryResultReceiver.done(consistency);
  }

  @Override
  public void cannotDecideOverCommitOrRollback() {
    recoveryResultReceiver.cannotDecideOverCommitOrRollback();
  }
}
