/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
package org.terracotta.nomad.client.change;

import org.terracotta.inet.HostPort;
import org.terracotta.nomad.client.Consistency;
import org.terracotta.nomad.client.results.AllResultsReceiver;
import org.terracotta.nomad.messages.DiscoverResponse;

import java.util.Collection;
import java.util.UUID;

public class ChangeAllResultsReceiverAdapter<T> implements AllResultsReceiver<T> {
  private final ChangeResultReceiver<T> changeResultReceiver;

  public ChangeAllResultsReceiverAdapter(ChangeResultReceiver<T> changeResultReceiver) {
    this.changeResultReceiver = changeResultReceiver;
  }

  @Override
  public void startDiscovery(Collection<HostPort> servers) {
    changeResultReceiver.startDiscovery(servers);
  }

  @Override
  public void discovered(HostPort server, DiscoverResponse<T> discovery) {
    changeResultReceiver.discovered(server, discovery);
  }

  @Override
  public void discoverFail(HostPort server, Throwable reason) {
    changeResultReceiver.discoverFail(server, reason);
  }

  @Override
  public void discoverConfigInconsistent(UUID changeUuid, Collection<HostPort> committedServers, Collection<HostPort> rolledBackServers) {
    changeResultReceiver.discoverConfigInconsistent(changeUuid, committedServers, rolledBackServers);
  }

  @Override
  public void discoverConfigPartitioned(Collection<Collection<HostPort>> partitions) {
    changeResultReceiver.discoverConfigPartitioned(partitions);
  }

  @Override
  public void endDiscovery() {
    changeResultReceiver.endDiscovery();
  }

  @Override
  public void startSecondDiscovery() {
    changeResultReceiver.startSecondDiscovery();
  }

  @Override
  public void discoverRepeated(HostPort server) {
    changeResultReceiver.discoverRepeated(server);
  }

  @Override
  public void discoverOtherClient(HostPort server, String lastMutationHost, String lastMutationUser) {
    changeResultReceiver.discoverOtherClient(server, lastMutationHost, lastMutationUser);
  }

  @Override
  public void endSecondDiscovery() {
    changeResultReceiver.endSecondDiscovery();
  }

  @Override
  public void discoverAlreadyPrepared(HostPort server, UUID changeUuid, String creationHost, String creationUser) {
    changeResultReceiver.discoverAlreadyPrepared(server, changeUuid, creationHost, creationUser);
  }

  @Override
  public void startPrepare(UUID newChangeUuid) {
    changeResultReceiver.startPrepare(newChangeUuid);
  }

  @Override
  public void prepared(HostPort server) {
    changeResultReceiver.prepared(server);
  }

  @Override
  public void prepareFail(HostPort server, Throwable reason) {
    changeResultReceiver.prepareFail(server, reason);
  }

  @Override
  public void prepareOtherClient(HostPort server, String lastMutationHost, String lastMutationUser) {
    changeResultReceiver.prepareOtherClient(server, lastMutationHost, lastMutationUser);
  }

  @Override
  public void prepareChangeUnacceptable(HostPort server, String rejectionReason) {
    changeResultReceiver.prepareChangeUnacceptable(server, rejectionReason);
  }

  @Override
  public void endPrepare() {
    changeResultReceiver.endPrepare();
  }

  @Override
  public void startTakeover() {
    throw new AssertionError("This should not be called during the change process");
  }

  @Override
  public void takeover(HostPort server) {
    throw new AssertionError("This should not be called during the change process");
  }

  @Override
  public void takeoverOtherClient(HostPort server, String lastMutationHost, String lastMutationUser) {
    throw new AssertionError("This should not be called during the change process");
  }

  @Override
  public void takeoverFail(HostPort server, Throwable reason) {
    throw new AssertionError("This should not be called during the change process");
  }

  @Override
  public void endTakeover() {
    throw new AssertionError("This should not be called during the change process");
  }

  @Override
  public void startCommit() {
    changeResultReceiver.startCommit();
  }

  @Override
  public void committed(HostPort server) {
    changeResultReceiver.committed(server);
  }

  @Override
  public void commitFail(HostPort server, Throwable reason) {
    changeResultReceiver.commitFail(server, reason);
  }

  @Override
  public void commitOtherClient(HostPort server, String lastMutationHost, String lastMutationUser) {
    changeResultReceiver.commitOtherClient(server, lastMutationHost, lastMutationUser);
  }

  @Override
  public void endCommit() {
    changeResultReceiver.endCommit();
  }

  @Override
  public void startRollback() {
    changeResultReceiver.startRollback();
  }

  @Override
  public void rolledBack(HostPort server) {
    changeResultReceiver.rolledBack(server);
  }

  @Override
  public void rollbackFail(HostPort server, Throwable reason) {
    changeResultReceiver.rollbackFail(server, reason);
  }

  @Override
  public void rollbackOtherClient(HostPort server, String lastMutationHost, String lastMutationUser) {
    changeResultReceiver.rollbackOtherClient(server, lastMutationHost, lastMutationUser);
  }

  @Override
  public void endRollback() {
    changeResultReceiver.endRollback();
  }

  @Override
  public void done(Consistency consistency) {
    changeResultReceiver.done(consistency);
  }

  @Override
  public void cannotDecideOverCommitOrRollback() {
    changeResultReceiver.cannotDecideOverCommitOrRollback();
  }
}
