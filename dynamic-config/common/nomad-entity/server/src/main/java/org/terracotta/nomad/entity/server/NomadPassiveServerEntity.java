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
import org.terracotta.entity.EntityUserException;
import org.terracotta.entity.InvokeContext;
import org.terracotta.entity.PassiveServerEntity;
import org.terracotta.monitoring.PlatformService;
import org.terracotta.monitoring.PlatformStopException;
import org.terracotta.nomad.entity.common.NomadEntityMessage;
import org.terracotta.nomad.entity.common.NomadEntityResponse;
import org.terracotta.nomad.messages.AcceptRejectResponse;
import org.terracotta.nomad.server.NomadException;
import org.terracotta.nomad.server.NomadServer;

public class NomadPassiveServerEntity<T> extends NomadCommonServerEntity<T> implements PassiveServerEntity<NomadEntityMessage, NomadEntityResponse> {
  private static final Logger LOGGER = LoggerFactory.getLogger(NomadPassiveServerEntity.class);

  private final PlatformService platformService;

  public NomadPassiveServerEntity(NomadServer<T> nomadServer, PlatformService platformService) {
    super(nomadServer);
    this.platformService = platformService;
  }

  @Override
  public void startSyncEntity() {
  }

  @Override
  public void endSyncEntity() {
  }

  @Override
  public void startSyncConcurrencyKey(int concurrencyKey) {
  }

  @Override
  public void endSyncConcurrencyKey(int concurrencyKey) {
  }

  @Override
  public void invokePassive(InvokeContext context, NomadEntityMessage message) throws EntityUserException {
    LOGGER.trace("invokePassive({})", message.getNomadMessage());
    try {
      AcceptRejectResponse response = processMessage(message.getNomadMessage());
      if (!response.isAccepted()) {
        LOGGER.error("Node was unable to commit Nomad message: {}. Response: {}", message, response);
        // Commit or rollback failed on passive: we restart it because we cannot do anything.
        // Upon restart, the passive server will sync with the active server and repair itself if needed if the active server is committed or rolled back.
        // The passive server will then restart with the wanted configuration without re-invoking the Nomad processors apply() methods of the commit phase.
        // But if the commit failed on teh active server too and the active server is in prepared state,
        // the passive server won't be able to repair itself and restart, it will be shutdown.
        // The active server will need to be repaired with the CLI repair command first.
        // This behavior is defined in DynamicConfigurationPassiveSync.
        platformService.stopPlatformIfPassive(PlatformService.RestartMode.STOP_AND_RESTART);
      }
    } catch (NomadException | RuntimeException | PlatformStopException e) {
      throw new EntityUserException(e.getMessage(), e);
    }
  }
}
