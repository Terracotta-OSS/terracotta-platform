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
package org.terracotta.dynamic_config.server.service;

import org.terracotta.diagnostic.model.LogicalServerState;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.nomad.DynamicConfigNomadChange;
import org.terracotta.dynamic_config.api.server.NomadPermissionChangeProcessor;
import org.terracotta.nomad.server.NomadException;
import org.terracotta.server.ServerJMX;

import java.util.EnumSet;

import static org.terracotta.diagnostic.model.LogicalServerState.ACTIVE;
import static org.terracotta.diagnostic.model.LogicalServerState.ACTIVE_RECONNECTING;
import static org.terracotta.diagnostic.model.LogicalServerState.DIAGNOSTIC;
import static org.terracotta.diagnostic.model.LogicalServerState.PASSIVE;
import static org.terracotta.diagnostic.model.LogicalServerState.SYNCHRONIZING;

/**
 * @author Mathieu Carbou
 */
class ServerStateCheck implements NomadPermissionChangeProcessor.Check {

  private static final EnumSet<LogicalServerState> ALLOWED = EnumSet.of(
      // we allow any config change if the node is in one of these state:
      ACTIVE,
      PASSIVE,
      ACTIVE_RECONNECTING,
      DIAGNOSTIC, // this mode is when a server is forced to start in diagnostic mode for repair,

      // we allow also any config change when a passive server is syncing its nomad append log from active server
      // note that this is only allowed here.
      // The NomadManager class client-side in the CLI does not allow to start any Nomad transaction if one of the server discovered is syncing.
      // If during a Nomad transaction a server is starting, this server won't be part of the transaction.
      // But this server will eventually become passive, then will get a replication message from active asking to commit
      // a change that it does not know. The passive will restart and sync again.
      SYNCHRONIZING // we can allow any change to happen if the server is
  );

  private final ServerJMX serverJMX;

  public ServerStateCheck(ServerJMX serverJMX) {
    this.serverJMX = serverJMX;
  }

  @Override
  public void check(NodeContext config, DynamicConfigNomadChange change) throws NomadException {
    LogicalServerState state = getLogicalServerState();
    if (!ALLOWED.contains(state)) {
      throw new NomadException("Unable to change configuration: the server is in state: " + state);
    }
  }

  private LogicalServerState getLogicalServerState() throws NomadException {
    try {
      return LogicalServerState.valueOf(validate(
          "DiagnosticExtensions", "getLogicalServerState",
          serverJMX.call("DiagnosticExtensions", "getLogicalServerState", null)));
    } catch (RuntimeException e) {
      throw new NomadException(e.getMessage(), e);
    }
  }

  private static String validate(String mBean, String method, String value) {
    if (value == null || value.startsWith("Invalid JMX")) {
      throw new IllegalStateException("mBean call '" + mBean + "#" + method + "' error: " + value);
    }
    return value;
  }
}
