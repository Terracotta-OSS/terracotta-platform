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

import org.terracotta.nomad.client.NomadClientProcess;
import org.terracotta.nomad.client.NomadDecider;
import org.terracotta.nomad.client.NomadEndpoint;
import org.terracotta.nomad.client.NomadMessageSender;
import org.terracotta.nomad.client.change.NomadChange;
import org.terracotta.nomad.client.results.AllResultsReceiver;
import org.terracotta.nomad.client.results.DiscoverResultsReceiver;

import java.time.Clock;
import java.util.List;

/**
 * @author Mathieu Carbou
 */
public class DiscoveryProcess<T> extends NomadClientProcess<NomadChange, T> {
  public DiscoveryProcess(List<NomadEndpoint<T>> servers, String host, String user, Clock clock) {
    super(servers, host, user, clock);
  }

  public void discover(DiscoverResultsReceiver<T> results) {
    runProcess(
        new DiscoveryAllResultsReceiverAdapter<>(results),
        new DiscoveryProcessDecider<>(),
        new DiscoveryMessageSender<>(servers, host, user, clock),
        null
    );
  }

  @Override
  protected boolean act(AllResultsReceiver<T> results, NomadDecider<T> decider, NomadMessageSender<T> messageSender, NomadChange change) {
    return false; // to return and not go to commit/rollback steps
  }
}
