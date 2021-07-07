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

import org.terracotta.common.struct.Version;
import org.terracotta.diagnostic.model.KitInformation;
import org.terracotta.diagnostic.model.LogicalServerState;
import org.terracotta.diagnostic.server.api.extension.DiagnosticExtensions;
import org.terracotta.server.ServerJMX;
import org.terracotta.server.ServerMBean;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.StandardMBean;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

  @Override
  public KitInformation getKitInformation() {
    String v = validate(
        MBEAN_SERVER, "getVersion",
        subsystem.call(MBEAN_SERVER, "getVersion", null)); // something like "Terracotta 5.8.2-pre6"
    String b = validate(
        MBEAN_SERVER, "getBuildID",
        subsystem.call(MBEAN_SERVER, "getBuildID", null)); // something like "2021-06-29 at 20:54:46 UTC (Revision 4450fe6fc2c174abd3528b8636b3296a6a79df00 from UNKNOWN)"

    int pos = v.indexOf(' ');
    String version = pos == -1 ? v : v.substring(pos + 1); // the moniker is hard-coded in core project and can be Terracotta or terracotta-enterprise

    Matcher sha = Pattern.compile(".*([0-9a-fA-F]{40}).*").matcher(b);
    String revision = sha.matches() ? sha.group(1) : "UNKNOWN";

    Matcher br = Pattern.compile(".* Revision [0-9a-fA-F]{40} from (.+)\\)").matcher(b);
    String branch = br.matches() ? br.group(1) : "UNKNOWN";

    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd 'at' HH:mm:ss z"); // from core
    Instant timestamp = dtf.parse(b.substring(0, 26), Instant::from);

    return new KitInformation(Version.valueOf(version), revision, branch, timestamp);
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
