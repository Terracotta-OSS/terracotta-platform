/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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

public enum LogicalServerState {

  /**
   * Active server has a reconnect window opened
   */
  ACTIVE_RECONNECTING("ACTIVE_RECONNECTING"),

  /**
   * Active server is blocked because of consistency votes
   */
  ACTIVE_SUSPENDED("ACTIVE_SUSPENDED"),

  /**
   * Passive server is blocked because of consistency votes
   */
  PASSIVE_SUSPENDED("PASSIVE_SUSPENDED"),

  /**
   * Passive server is up and ready to replicate
   */
  PASSIVE("PASSIVE", "PASSIVE-STANDBY"),

  /**
   * Passive server is up and ready for relay sync
   */
  PASSIVE_RELAY("PASSIVE_RELAY", "PASSIVE-RELAY"),

  /**
   * Active server is ready to receive clients
   */
  ACTIVE("ACTIVE", "ACTIVE-COORDINATOR"),

  /**
   * Status returned when parsing failed
   */
  UNKNOWN("UNKNOWN"),

  /**
   * When a server is not reachable, this will be the status used
   */
  UNREACHABLE("UNREACHABLE"),

  /**
   * Server is bootstrapping
   */
  STARTING("STARTING", "START_STATE", "START-STATE"),

  /**
   * Server startup is suspended
   */
  START_SUSPENDED("START_SUSPENDED"),

  /**
   * A fresh server
   */
  UNINITIALIZED("UNINITIALIZED", "PASSIVE-UNINITIALIZED"),

  /**
   * Passive server is synchronizing with active server
   */
  SYNCHRONIZING("SYNCHRONIZING", "PASSIVE-SYNCING"),

  /**
   * Server is not yet configured
   */
  DIAGNOSTIC("DIAGNOSTIC");

  private final String[] mappings;

  LogicalServerState(String... mappings) {
    this.mappings = mappings == null ? new String[0] : mappings;
  }

  public static LogicalServerState parse(String value) {
    for (LogicalServerState serverState : LogicalServerState.values()) {
      for (String mapping : serverState.mappings) {
        if (mapping.equalsIgnoreCase(value)) {
          return serverState;
        }
      }
    }
    return UNKNOWN;
  }

  public static LogicalServerState from(String state, boolean reconnectWindow, boolean isBlocked) {
    LogicalServerState parsedLogicalServerState = parse(state);
    if (parsedLogicalServerState == LogicalServerState.ACTIVE && reconnectWindow) {
      return LogicalServerState.ACTIVE_RECONNECTING;
    } else if (parsedLogicalServerState == LogicalServerState.ACTIVE && isBlocked) {
      return LogicalServerState.ACTIVE_SUSPENDED;
    } else if (parsedLogicalServerState == LogicalServerState.PASSIVE && isBlocked) {
      return LogicalServerState.PASSIVE_SUSPENDED;
    } else if (parsedLogicalServerState == LogicalServerState.STARTING && isBlocked) {
      return LogicalServerState.START_SUSPENDED;
    } else return parsedLogicalServerState;
  }

  public boolean canConnect() {
    return this == ACTIVE;
  }

  public boolean isUnknown() {
    return this == UNKNOWN;
  }

  public boolean isUnreacheable() {
    return this == UNREACHABLE;
  }

  public boolean isActive() {
    return this == ACTIVE || this == ACTIVE_RECONNECTING;
  }

  public boolean isStarting() {
    return this == STARTING;
  }

  public boolean isSynchronizing() {
    return this == SYNCHRONIZING;
  }

  public boolean isPassive() {
    return this == PASSIVE;
  }

  public boolean isBlocked() {
    return this == START_SUSPENDED || this == ACTIVE_SUSPENDED || this == PASSIVE_SUSPENDED;
  }
}
