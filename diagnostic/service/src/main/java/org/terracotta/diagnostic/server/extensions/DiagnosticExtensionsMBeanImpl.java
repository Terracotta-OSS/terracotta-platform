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

import org.terracotta.diagnostic.model.LogicalServerState;
import org.terracotta.diagnostic.server.api.extension.DiagnosticExtensions;
import org.terracotta.server.ServerJMX;
import org.terracotta.server.ServerMBean;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.StandardMBean;
import java.util.Set;

import static org.terracotta.diagnostic.common.DiagnosticConstants.MBEAN_CONSISTENCY_MANAGER;
import static org.terracotta.diagnostic.common.DiagnosticConstants.MBEAN_DIAGNOSTIC_EXTENSIONS;
import static org.terracotta.diagnostic.common.DiagnosticConstants.MBEAN_SERVER;
import static org.terracotta.diagnostic.common.DiagnosticConstants.MESSAGE_INVALID_JMX;

public class DiagnosticExtensionsMBeanImpl extends StandardMBean implements org.terracotta.server.ServerMBean, DiagnosticExtensions {
  private final ServerJMX subsystem;

  public DiagnosticExtensionsMBeanImpl(ServerJMX subsystem) {
    super(DiagnosticExtensions.class, false);
    this.subsystem = subsystem;
  }

  public void expose() {
    subsystem.registerMBean(MBEAN_DIAGNOSTIC_EXTENSIONS, this);
  }

  @Override
  public LogicalServerState getLogicalServerState() {
    boolean isBlocked = hasConsistencyManager() && isBlocked();
    boolean isReconnectWindow = isReconnectWindow();
    final String state = getState();
    return LogicalServerState.from(state, isReconnectWindow, isBlocked);
  }

  boolean hasConsistencyManager() {
    try {
      Set<ObjectInstance> matchingBeans = subsystem.getMBeanServer().queryMBeans(
          ServerMBean.createMBeanName(MBEAN_CONSISTENCY_MANAGER), null);
      return matchingBeans.iterator().hasNext();
    } catch (MalformedObjectNameException e) {
      throw new IllegalStateException(e);
    }
  }

  private boolean isBlocked() {
    return Boolean.parseBoolean(validate(
        MBEAN_CONSISTENCY_MANAGER, "isBlocked",
        subsystem.call(MBEAN_CONSISTENCY_MANAGER, "isBlocked", null)
    ));
  }

  private boolean isReconnectWindow() {
    return Boolean.parseBoolean(validate(
        MBEAN_SERVER, "isReconnectWindow",
        subsystem.call(MBEAN_SERVER, "isReconnectWindow", null)
    ));
  }

  private String getState() {
    return validate(
        MBEAN_SERVER, "getState",
        subsystem.call(MBEAN_SERVER, "getState", null));
  }

  private static String validate(String mBean, String method, String value) {
    if (value == null || value.startsWith(MESSAGE_INVALID_JMX)) {
      throw new IllegalStateException("mBean call '" + mBean + "#" + method + "' error: " + value);
    }
    return value;
  }
}
