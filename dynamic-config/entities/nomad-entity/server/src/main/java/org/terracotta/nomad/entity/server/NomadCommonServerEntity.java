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
package org.terracotta.nomad.entity.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.entity.CommonServerEntity;
import org.terracotta.nomad.entity.common.NomadEntityMessage;
import org.terracotta.nomad.entity.common.NomadEntityResponse;
import org.terracotta.nomad.messages.AcceptRejectResponse;
import org.terracotta.nomad.messages.CommitMessage;
import org.terracotta.nomad.messages.MutativeMessage;
import org.terracotta.nomad.messages.PrepareMessage;
import org.terracotta.nomad.messages.RollbackMessage;
import org.terracotta.nomad.messages.TakeoverMessage;
import org.terracotta.nomad.server.NomadException;
import org.terracotta.nomad.server.NomadServer;

public class NomadCommonServerEntity<T> implements CommonServerEntity<NomadEntityMessage, NomadEntityResponse> {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  private final NomadServer<T> nomadServer;

  public NomadCommonServerEntity(NomadServer<T> nomadServer) {
    this.nomadServer = nomadServer;
  }

  @Override
  public final void createNew() {
  }

  @Override
  public final void destroy() {
  }

  protected AcceptRejectResponse processMessage(MutativeMessage nomadMessage) throws NomadException {
    logger.trace("Processing Nomad message: {}", nomadMessage);
    AcceptRejectResponse response;
    if (nomadMessage instanceof CommitMessage) {
      response = nomadServer.commit((CommitMessage) nomadMessage);
    } else if (nomadMessage instanceof RollbackMessage) {
      response = nomadServer.rollback((RollbackMessage) nomadMessage);
    } else if (nomadMessage instanceof TakeoverMessage) {
      response = nomadServer.takeover((TakeoverMessage) nomadMessage);
    } else if (nomadMessage instanceof PrepareMessage) {
      response = nomadServer.prepare((PrepareMessage) nomadMessage);
    } else {
      throw new IllegalArgumentException("Unsupported Nomad message: " + nomadMessage.getClass().getName());
    }
    logger.trace("Result: {}", response);
    return response;
  }
}
