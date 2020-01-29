/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
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
  String MBEAN_CONSISTENCY_MANAGER = "ConsistencyManager";
  String MBEAN_SHUTDOWN = "Shutdown";
  String MBEAN_L2_DUMPER = "L2Dumper";
  String MBEAN_IP_WHITELIST = "IPWhitelist";
  String MBEAN_BACKUP = "Backup";
  String MBEAN_SERVER = "Server";
  String MBEAN_NOMAD = "Nomad";
}
