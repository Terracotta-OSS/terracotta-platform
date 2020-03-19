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
package org.terracotta.diagnostic.common;

/**
 * @author Mathieu Carbou
 */
public interface DiagnosticConstants {
  String MESSAGE_UNKNOWN_COMMAND = "UNKNOWN CMD";
  String MESSAGE_INVALID_JMX = "Invalid JMX"; // will handle invalid JMS calls, invalid JMX attributes, etc. See JMXSubsystem and DiagnosticsHandler.
  String MESSAGE_REQUEST_TIMEOUT = "Request Timeout";
  String MESSAGE_NOT_PERMITTED = "NOT PERMITTED";
  String MESSAGE_NULL_RETURN = "";

  String MBEAN_DIAGNOSTIC_REQUEST_HANDLER = "DiagnosticRequestHandler";
  String MBEAN_DETAILED_SERVER_STATE = "DetailedServerState";
  String MBEAN_CONSISTENCY_MANAGER = "ConsistencyManager";
  String MBEAN_SHUTDOWN = "Shutdown";
  String MBEAN_L2_DUMPER = "L2Dumper";
  String MBEAN_SERVER = "Server";
}
