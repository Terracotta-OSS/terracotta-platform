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
package org.terracotta.diagnostic.server.extensions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.diagnostic.model.LogicalServerState;
import org.terracotta.diagnostic.server.api.extension.LogicalServerStateProvider;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import java.util.Set;

import static java.lang.Boolean.parseBoolean;
import javax.management.StandardMBean;
import static org.terracotta.diagnostic.common.DiagnosticConstants.MBEAN_CONSISTENCY_MANAGER;
import static org.terracotta.diagnostic.common.DiagnosticConstants.MBEAN_LOGICAL_SERVER_STATE;
import static org.terracotta.diagnostic.common.DiagnosticConstants.MBEAN_SERVER;
import static org.terracotta.diagnostic.common.DiagnosticConstants.MESSAGE_INVALID_JMX;
import org.terracotta.server.ServerEnv;
import org.terracotta.server.ServerJMX;
import org.terracotta.server.ServerMBean;

public class LogicalServerStateMBeanImpl extends StandardMBean implements org.terracotta.server.ServerMBean, LogicalServerStateProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(LogicalServerStateMBeanImpl.class);
  private final ServerJMX subsystem;

  public LogicalServerStateMBeanImpl(ServerJMX subsystem) {
    super(LogicalServerStateProvider.class, false);
    this.subsystem = subsystem;
  }

  public void expose() {
    ServerEnv.getServer().getManagement().registerMBean(MBEAN_LOGICAL_SERVER_STATE, this);
  }

  @Override
  public LogicalServerState getLogicalServerState() {
    boolean isBlocked = hasConsistencyManager() && parseBoolean(isBlocked());
    boolean isReconnectWindow = parseBoolean(isReconnectWindow());
    return enhanceServerState(getState(), isReconnectWindow, isBlocked);
  }

  private LogicalServerState enhanceServerState(String state, boolean reconnectWindow, boolean isBlocked) {
    // subsystem call failed
    if (state == null || state.startsWith(MESSAGE_INVALID_JMX)) {
      LOGGER.error("A server returned the following invalid state: {}", state == null ? "null" : state);
      return LogicalServerState.UNKNOWN;
    }

    // enhance default server state
    return LogicalServerState.from(state, reconnectWindow, isBlocked);
  }

  boolean hasConsistencyManager() {
    try {
      Set<ObjectInstance> matchingBeans = ServerEnv.getServer().getManagement().getMBeanServer().queryMBeans(
          ServerMBean.createMBeanName(MBEAN_CONSISTENCY_MANAGER), null);
      return matchingBeans.iterator().hasNext();
    } catch (MalformedObjectNameException e) {
      // really not supposed to happen
      LOGGER.error("Invalid MBean name: {}", MBEAN_CONSISTENCY_MANAGER, e);
      return false;
    }
  }

  private String isBlocked() {
    return subsystem.call(MBEAN_CONSISTENCY_MANAGER, "isBlocked", null);
  }

  private String isReconnectWindow() {
    return subsystem.call(MBEAN_SERVER, "isReconnectWindow", null);
  }

  private String getState() {
    return subsystem.call(MBEAN_SERVER, "getState", null);
  }
}
