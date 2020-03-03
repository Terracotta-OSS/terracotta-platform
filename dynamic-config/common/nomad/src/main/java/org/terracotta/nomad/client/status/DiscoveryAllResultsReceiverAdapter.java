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
package org.terracotta.nomad.client.status;

import org.terracotta.nomad.client.Consistency;
import org.terracotta.nomad.client.results.AllResultsReceiver;
import org.terracotta.nomad.client.results.DiscoverResultsReceiver;
import org.terracotta.nomad.messages.DiscoverResponse;

import java.net.InetSocketAddress;
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
  public void startDiscovery(Collection<InetSocketAddress> servers) {
    receiver.startDiscovery(servers);
  }

  @Override
  public void discovered(InetSocketAddress server, DiscoverResponse<T> discovery) {
    receiver.discovered(server, discovery);
  }

  @Override
  public void discoverFail(InetSocketAddress server, String reason) {
    receiver.discoverFail(server, reason);
  }

  @Override
  public void discoverClusterInconsistent(UUID changeUuid, Collection<InetSocketAddress> committedServers, Collection<InetSocketAddress> rolledBackServers) {
    receiver.discoverClusterInconsistent(changeUuid, committedServers, rolledBackServers);
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
  public void discoverRepeated(InetSocketAddress server) {
    receiver.discoverRepeated(server);
  }

  @Override
  public void discoverOtherClient(InetSocketAddress server, String lastMutationHost, String lastMutationUser) {
    receiver.discoverOtherClient(server, lastMutationHost, lastMutationUser);
  }

  @Override
  public void endSecondDiscovery() {
    receiver.endSecondDiscovery();
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
}
