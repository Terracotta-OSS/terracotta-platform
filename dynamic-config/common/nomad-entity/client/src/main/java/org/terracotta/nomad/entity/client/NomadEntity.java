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
package org.terracotta.nomad.entity.client;

import org.terracotta.connection.entity.Entity;
import org.terracotta.nomad.messages.AcceptRejectResponse;
import org.terracotta.nomad.messages.CommitMessage;
import org.terracotta.nomad.messages.DiscoverResponse;
import org.terracotta.nomad.messages.MutativeMessage;
import org.terracotta.nomad.messages.PrepareMessage;
import org.terracotta.nomad.messages.RollbackMessage;
import org.terracotta.nomad.messages.TakeoverMessage;
import org.terracotta.nomad.server.NomadException;
import org.terracotta.nomad.server.NomadServer;

import java.time.Duration;

/**
 * @author Mathieu Carbou
 */
public interface NomadEntity<T> extends Entity, NomadServer<T> {
  @Override
  default DiscoverResponse<T> discover() {
    throw new UnsupportedOperationException();
  }

  @Override
  default AcceptRejectResponse prepare(PrepareMessage message) throws NomadException {
    throw new UnsupportedOperationException();
  }

  @Override
  default AcceptRejectResponse commit(CommitMessage message) throws NomadException {
    return send(message);
  }

  @Override
  default AcceptRejectResponse rollback(RollbackMessage message) throws NomadException {
    return send(message);
  }

  @Override
  default AcceptRejectResponse takeover(TakeoverMessage message) throws NomadException {
    return send(message);
  }

  AcceptRejectResponse send(MutativeMessage message) throws NomadException;

  class Settings {
    private Duration requestTimeout = Duration.ofSeconds(20);

    public Duration getRequestTimeout() {
      return requestTimeout;
    }

    public Settings setRequestTimeout(Duration requestTimeout) {
      this.requestTimeout = requestTimeout;
      return this;
    }
  }
}
