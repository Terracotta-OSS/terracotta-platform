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
package org.terracotta.nomad.client.status;

import org.terracotta.nomad.client.NomadEndpoint;
import org.terracotta.nomad.client.NomadMessageSender;
import org.terracotta.nomad.client.change.NomadChange;
import org.terracotta.nomad.client.results.CommitResultsReceiver;
import org.terracotta.nomad.client.results.PrepareResultsReceiver;
import org.terracotta.nomad.client.results.RollbackResultsReceiver;
import org.terracotta.nomad.client.results.TakeoverResultsReceiver;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * @author Mathieu Carbou
 */
public class DiscoveryMessageSender<T> extends NomadMessageSender<T> {
  public DiscoveryMessageSender(List<NomadEndpoint<T>> servers, String host, String user, Clock clock) {
    super(servers, host, user, clock);
  }

  @Override
  public void sendPrepares(PrepareResultsReceiver results, UUID changeUuid, NomadChange change) {
    // ensure we do nothing
  }

  @Override
  public void sendCommits(CommitResultsReceiver results) {
    // ensure we do nothing
  }

  @Override
  public void sendRollbacks(RollbackResultsReceiver results) {
    // ensure we do nothing
  }

  @Override
  public void sendTakeovers(TakeoverResultsReceiver results) {
    // ensure we do nothing
  }
}
