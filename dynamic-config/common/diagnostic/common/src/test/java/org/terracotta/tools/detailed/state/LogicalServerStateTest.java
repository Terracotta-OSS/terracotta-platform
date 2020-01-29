/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.tools.detailed.state;

import org.junit.Test;
import org.terracotta.diagnostic.common.LogicalServerState;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.terracotta.diagnostic.common.LogicalServerState.ACTIVE;
import static org.terracotta.diagnostic.common.LogicalServerState.ACTIVE_RECONNECTING;
import static org.terracotta.diagnostic.common.LogicalServerState.ACTIVE_SUSPENDED;
import static org.terracotta.diagnostic.common.LogicalServerState.PASSIVE;
import static org.terracotta.diagnostic.common.LogicalServerState.PASSIVE_SUSPENDED;
import static org.terracotta.diagnostic.common.LogicalServerState.START_SUSPENDED;
import static org.terracotta.diagnostic.common.LogicalServerState.SYNCHRONIZING;
import static org.terracotta.diagnostic.common.LogicalServerState.UNKNOWN;

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