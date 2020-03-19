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

import com.tc.objectserver.impl.JMXSubsystem;
import org.junit.Before;
import org.junit.Test;

import javax.management.NotCompliantMBeanException;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.terracotta.diagnostic.common.DiagnosticConstants.MBEAN_CONSISTENCY_MANAGER;
import static org.terracotta.diagnostic.common.DiagnosticConstants.MBEAN_SERVER;
import static org.terracotta.diagnostic.common.DiagnosticConstants.MESSAGE_INVALID_JMX;
import static org.terracotta.diagnostic.common.LogicalServerState.ACTIVE;
import static org.terracotta.diagnostic.common.LogicalServerState.ACTIVE_RECONNECTING;
import static org.terracotta.diagnostic.common.LogicalServerState.ACTIVE_SUSPENDED;
import static org.terracotta.diagnostic.common.LogicalServerState.UNKNOWN;

public class DetailedServerStateTest {
  private JMXSubsystem jmxSubsystem;
  private DetailedServerStateImpl detailedServerState;

  @Before
  public void setUp() throws NotCompliantMBeanException {
    jmxSubsystem = mock(JMXSubsystem.class);
    detailedServerState = new DetailedServerStateImpl(jmxSubsystem) {
      @Override
      boolean hasConsistencyManager() {
        return true;
      }
    };
  }

  @Test
  public void getDetailedServerState_jmx_down() {
    when(jmxSubsystem.call(MBEAN_CONSISTENCY_MANAGER, "isBlocked", null)).thenReturn(String.valueOf(false));
    when(jmxSubsystem.call(MBEAN_SERVER, "isReconnectWindow", null)).thenReturn(MESSAGE_INVALID_JMX);
    when(jmxSubsystem.call(MBEAN_SERVER, "getState", null)).thenReturn(MESSAGE_INVALID_JMX);

    assertThat(detailedServerState.getLogicalServerState(), equalTo(UNKNOWN));
  }

  @Test
  public void getDetailedServerState_reconnect_window() {
    when(jmxSubsystem.call(MBEAN_CONSISTENCY_MANAGER, "isBlocked", null)).thenReturn(String.valueOf(false));
    when(jmxSubsystem.call(MBEAN_SERVER, "isReconnectWindow", null)).thenReturn(String.valueOf(true));
    when(jmxSubsystem.call(MBEAN_SERVER, "getState", null)).thenReturn("ACTIVE");

    assertThat(detailedServerState.getLogicalServerState(), equalTo(ACTIVE_RECONNECTING));
  }

  @Test
  public void getDetailedServerState_active_is_blocked() {
    when(jmxSubsystem.call(MBEAN_CONSISTENCY_MANAGER, "isBlocked", null)).thenReturn(String.valueOf(true));
    when(jmxSubsystem.call(MBEAN_SERVER, "isReconnectWindow", null)).thenReturn(String.valueOf(false));
    when(jmxSubsystem.call(MBEAN_SERVER, "getState", null)).thenReturn("ACTIVE");

    assertThat(detailedServerState.getLogicalServerState(), equalTo(ACTIVE_SUSPENDED));
  }

  @Test
  public void getDetailedServerState_all_good() {
    when(jmxSubsystem.call(MBEAN_CONSISTENCY_MANAGER, "isBlocked", null)).thenReturn(String.valueOf(false));
    when(jmxSubsystem.call(MBEAN_SERVER, "isReconnectWindow", null)).thenReturn(String.valueOf(false));
    when(jmxSubsystem.call(MBEAN_SERVER, "getState", null)).thenReturn("ACTIVE");

    assertThat(detailedServerState.getLogicalServerState(), equalTo(ACTIVE));
  }
}