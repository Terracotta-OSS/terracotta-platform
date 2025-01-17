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
package org.terracotta.nomad.client.change;

import org.terracotta.inet.HostPort;
import org.terracotta.nomad.client.NomadEndpoint;
import org.terracotta.nomad.client.NomadMessageSender;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

public class ChangeMessageSender<T> extends NomadMessageSender<T> {
  public ChangeMessageSender(List<NomadEndpoint<T>> servers, String host, String user, Clock clock) {
    super(servers, host, user, clock);
  }

  @Override
  public void startPrepare(UUID newChangeUuid) {
    super.startPrepare(newChangeUuid);
    this.changeUuid = newChangeUuid;
  }

  @Override
  public void prepared(HostPort server) {
    super.prepared(server);
    registerPreparedServer(server);
  }

  @Override
  public void prepareChangeUnacceptable(HostPort server, String rejectionReason) {
    // See comment in NomadServerImpl
    // a node is prepared if its change has been written on disk but not yet committed,
    // regardless of the change was accepted or rejected
    registerPreparedServer(server);
  }
}
