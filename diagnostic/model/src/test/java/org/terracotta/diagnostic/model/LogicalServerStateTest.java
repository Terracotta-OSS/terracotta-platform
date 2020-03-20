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
package org.terracotta.diagnostic.model;

import org.junit.Test;
import org.terracotta.diagnostic.model.LogicalServerState;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.terracotta.diagnostic.model.LogicalServerState.ACTIVE;
import static org.terracotta.diagnostic.model.LogicalServerState.ACTIVE_RECONNECTING;
import static org.terracotta.diagnostic.model.LogicalServerState.ACTIVE_SUSPENDED;
import static org.terracotta.diagnostic.model.LogicalServerState.PASSIVE;
import static org.terracotta.diagnostic.model.LogicalServerState.PASSIVE_SUSPENDED;
import static org.terracotta.diagnostic.model.LogicalServerState.START_SUSPENDED;
import static org.terracotta.diagnostic.model.LogicalServerState.SYNCHRONIZING;
import static org.terracotta.diagnostic.model.LogicalServerState.UNKNOWN;

public class LogicalServerStateTest {

  @Test
  public void parseTest_unknown() {
    assertThat(LogicalServerState.parse("BLOUP"), equalTo(UNKNOWN));
  }

  @Test
  public void parseTest_active() {
    assertThat(LogicalServerState.parse("ACTIVE-COORDINATOR"), equalTo(ACTIVE));
  }

  @Test
  public void parseTest_passive() {
    assertThat(LogicalServerState.parse("PASSIVE-STANDBY"), equalTo(PASSIVE));
  }

  @Test
  public void enhanceServerState_active_suspended() {
    assertThat(LogicalServerState.from("ACTIVE-COORDINATOR", false, true), equalTo(ACTIVE_SUSPENDED));
  }

  @Test
  public void enhanceServerState_passive_suspended() {
    assertThat(LogicalServerState.from("PASSIVE-STANDBY", false, true), equalTo(PASSIVE_SUSPENDED));
  }

  @Test
  public void enhanceServerState_start_suspended() {
    assertThat(LogicalServerState.from("START-STATE", false, true), equalTo(START_SUSPENDED));
  }

  @Test
  public void enhanceServerState_active_reconnecting() {
    assertThat(LogicalServerState.from("ACTIVE-COORDINATOR", true, false), equalTo(ACTIVE_RECONNECTING));
  }

  @Test
  public void enhanceServerState_invalid() {
    assertThat(LogicalServerState.from("Invalid JMX call", false, false), equalTo(UNKNOWN));
  }

  @Test
  public void enhanceServerState_null() {
    assertThat(LogicalServerState.from(null, false, false), equalTo(UNKNOWN));
  }

  @Test
  public void enhanceServerState_other() {
    assertThat(LogicalServerState.from("PASSIVE-SYNCING", false, false), equalTo(SYNCHRONIZING));
  }

}