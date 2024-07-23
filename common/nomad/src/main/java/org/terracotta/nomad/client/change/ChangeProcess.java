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

import org.terracotta.nomad.client.NomadClientProcess;
import org.terracotta.nomad.client.NomadDecider;
import org.terracotta.nomad.client.NomadEndpoint;
import org.terracotta.nomad.client.NomadMessageSender;
import org.terracotta.nomad.client.results.AllResultsReceiver;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

public class ChangeProcess<T> extends NomadClientProcess<NomadChange, T> {
  public ChangeProcess(List<NomadEndpoint<T>> servers, String host, String user, Clock clock) {
    super(servers, host, user, clock);
  }

  public void applyChange(ChangeResultReceiver<T> results, NomadChange change) {
    runProcess(
        new ChangeAllResultsReceiverAdapter<>(results),
        new ChangeProcessDecider<>(),
        new ChangeMessageSender<>(servers, host, user, clock),
        change
    );
  }

  @Override
  protected boolean act(AllResultsReceiver<T> results, NomadDecider<T> decider, NomadMessageSender<T> messageSender, NomadChange change) {
    UUID changeUuid = UUID.randomUUID();
    messageSender.sendPrepares(results, changeUuid, change);
    return true;
  }
}
