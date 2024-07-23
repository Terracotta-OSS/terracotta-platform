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
package org.terracotta.nomad.client.status;

import org.terracotta.inet.HostPort;
import org.terracotta.nomad.client.Consistency;
import org.terracotta.nomad.client.results.AllResultsReceiver;
import org.terracotta.nomad.client.results.DiscoverResultsReceiver;
import org.terracotta.nomad.messages.DiscoverResponse;

import java.util.Collection;
import java.util.UUID;

/**
 * @author Mathieu Carbou
 */
public class DiscoveryAllResultsReceiverAdapter<T> implements AllResultsReceiver<T> {
  private final DiscoverResultsReceiver<T> receiver;

  public DiscoveryAllResultsReceiverAdapter(DiscoverResultsReceiver<T> receiver) {
    this.receiver = receiver;
  }

  @Override
  public void done(Consistency consistency) {
    receiver.done(consistency);
  }

  @Override
  public void cannotDecideOverCommitOrRollback() {
    receiver.cannotDecideOverCommitOrRollback();
  }

  @Override
  public void startDiscovery(Collection<HostPort> servers) {
    receiver.startDiscovery(servers);
  }

  @Override
  public void discovered(HostPort server, DiscoverResponse<T> discovery) {
    receiver.discovered(server, discovery);
  }

  @Override
  public void discoverFail(HostPort server, Throwable reason) {
    receiver.discoverFail(server, reason);
  }

  @Override
  public void discoverConfigInconsistent(UUID changeUuid, Collection<HostPort> committedServers, Collection<HostPort> rolledBackServers) {
    receiver.discoverConfigInconsistent(changeUuid, committedServers, rolledBackServers);
  }

  @Override
  public void discoverConfigPartitioned(Collection<Collection<HostPort>> partitions) {
    receiver.discoverConfigPartitioned(partitions);
  }

  @Override
  public void endDiscovery() {
    receiver.endDiscovery();
  }

  @Override
  public void startSecondDiscovery() {
    receiver.startSecondDiscovery();
  }

  @Override
  public void discoverRepeated(HostPort server) {
    receiver.discoverRepeated(server);
  }

  @Override
  public void discoverOtherClient(HostPort server, String lastMutationHost, String lastMutationUser) {
    receiver.discoverOtherClient(server, lastMutationHost, lastMutationUser);
  }

  @Override
  public void endSecondDiscovery() {
    receiver.endSecondDiscovery();
  }

  @Override
  public void discoverAlreadyPrepared(HostPort server, UUID changeUuid, String creationHost, String creationUser) {
    throw new AssertionError("This should not be called during the recovery process");
  }

  @Override
  public void startPrepare(UUID newChangeUuid) {
    throw new AssertionError("This should not be called during the recovery process");
  }

  @Override
  public void prepared(HostPort server) {
    throw new AssertionError("This should not be called during the recovery process");
  }

  @Override
  public void prepareFail(HostPort server, Throwable reason) {
    throw new AssertionError("This should not be called during the recovery process");
  }

  @Override
  public void prepareOtherClient(HostPort server, String lastMutationHost, String lastMutationUser) {
    throw new AssertionError("This should not be called during the recovery process");
  }

  @Override
  public void prepareChangeUnacceptable(HostPort server, String rejectionReason) {
    throw new AssertionError("This should not be called during the recovery process");
  }

  @Override
  public void endPrepare() {
    throw new AssertionError("This should not be called during the recovery process");
  }
}
