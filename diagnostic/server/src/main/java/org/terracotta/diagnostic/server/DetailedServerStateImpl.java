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
package org.terracotta.diagnostic.server;

import com.tc.management.AbstractTerracottaMBean;
import com.tc.management.TerracottaManagement;
import com.tc.objectserver.impl.JMXSubsystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.server.api.DetailedServerState;
import org.terracotta.dynamic_config.server.api.LogicalServerState;

import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectInstance;
import java.lang.management.ManagementFactory;
import java.util.Set;

import static com.tc.management.TerracottaManagement.MBeanDomain.PUBLIC;
import static java.lang.Boolean.parseBoolean;
import static org.terracotta.diagnostic.common.DiagnosticConstants.MBEAN_CONSISTENCY_MANAGER;
import static org.terracotta.diagnostic.common.DiagnosticConstants.MBEAN_SERVER;
import static org.terracotta.diagnostic.common.DiagnosticConstants.MESSAGE_INVALID_JMX;

public class DetailedServerStateImpl extends AbstractTerracottaMBean implements DetailedServerState {
  private static final Logger LOGGER = LoggerFactory.getLogger(DetailedServerStateImpl.class);
  private final JMXSubsystem subsystem;

  public static DetailedServerStateImpl init() {
    try {
      return new DetailedServerStateImpl(new JMXSubsystem());
    } catch (NotCompliantMBeanException e) {
      throw new AssertionError(e);
    }
  }

  DetailedServerStateImpl(JMXSubsystem jmxSubsystem) throws NotCompliantMBeanException {
    super(DetailedServerState.class, false);
    this.subsystem = jmxSubsystem;
  }

  @Override
  public LogicalServerState getLogicalServerState() {
    boolean isBlocked = hasConsistencyManager() && parseBoolean(isBlocked());
    boolean isReconnectWindow = parseBoolean(isReconnectWindow());
    return enhanceServerState(getState(), isReconnectWindow, isBlocked);
  }

  @Override
  public void reset() {
    // nothing
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
      Set<ObjectInstance> matchingBeans = ManagementFactory.getPlatformMBeanServer().queryMBeans(
          TerracottaManagement.createObjectName(null, MBEAN_CONSISTENCY_MANAGER, PUBLIC), null);
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
