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
package org.terracotta.nomad.client;

import org.terracotta.nomad.client.change.ChangeProcess;
import org.terracotta.nomad.client.change.ChangeResultReceiver;
import org.terracotta.nomad.client.change.NomadChange;
import org.terracotta.nomad.client.recovery.RecoveryProcess;
import org.terracotta.nomad.client.recovery.RecoveryResultReceiver;
import org.terracotta.nomad.client.results.DiscoverResultsReceiver;
import org.terracotta.nomad.client.status.DiscoveryProcess;
import org.terracotta.nomad.server.ChangeRequestState;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.time.Clock;
import java.util.List;

public class NomadClient<T> implements AutoCloseable {
  private final List<NomadEndpoint<T>> servers;
  private final String host;
  private final String user;
  private final Clock clock;

  /**
   * @param servers the set of servers to run the Nomad protocol across
   * @param host    the name of the local machine
   * @param user    the name of the user the current process is running as
   */
  @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
   public NomadClient(List<NomadEndpoint<T>> servers, String host, String user, Clock clock) {
    this.clock = clock;
    if (servers.isEmpty()) {
      throw new IllegalArgumentException("There must be at least one server");
    }

    this.servers = servers;
    this.host = host;
    this.user = user;
  }

  public void tryApplyChange(ChangeResultReceiver<T> results, NomadChange change) {
    ChangeProcess<T> changeProcess = new ChangeProcess<>(servers, host, user, clock);
    changeProcess.applyChange(results, change);
  }

  public void tryRecovery(RecoveryResultReceiver<T> results, int expectedTotalNodeCount, ChangeRequestState forcedState) {
    RecoveryProcess<T> recoveryProcess = new RecoveryProcess<>(servers, host, user, clock);
    recoveryProcess.recover(results, expectedTotalNodeCount, forcedState);
  }

  public void tryDiscovery(DiscoverResultsReceiver<T> results) {
    DiscoveryProcess<T> discoveryProcess = new DiscoveryProcess<>(servers, host, user, clock);
    discoveryProcess.discover(results);
  }

  @Override
  public void close() {
    RuntimeException error = null;
    for (NomadEndpoint<T> server : servers) {
      try {
        server.close();
      } catch (RuntimeException e) {
        if (error == null) {
          error = e;
        } else {
          error.addSuppressed(e);
        }
      }
    }
    if (error != null) {
      throw error;
    }
  }
}
