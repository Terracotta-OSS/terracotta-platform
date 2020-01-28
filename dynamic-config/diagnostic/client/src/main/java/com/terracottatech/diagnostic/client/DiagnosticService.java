/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.diagnostic.client;

import com.terracottatech.tools.detailed.state.LogicalServerState;

import java.io.Closeable;

import static com.terracottatech.diagnostic.common.DiagnosticConstants.MBEAN_BACKUP;
import static com.terracottatech.diagnostic.common.DiagnosticConstants.MBEAN_CONSISTENCY_MANAGER;
import static com.terracottatech.diagnostic.common.DiagnosticConstants.MBEAN_IP_WHITELIST;
import static com.terracottatech.diagnostic.common.DiagnosticConstants.MBEAN_L2_DUMPER;
import static com.terracottatech.diagnostic.common.DiagnosticConstants.MBEAN_SERVER;
import static com.terracottatech.diagnostic.common.DiagnosticConstants.MBEAN_SHUTDOWN;

/**
 * This class can be used as a replacement of the {@link com.terracotta.diagnostic.Diagnostics} class.
 * <p>
 * This class supports exception handling and complex request/response objects thanks to the {@link #getProxy(Class)} method.
 * <p>
 * It also allows to commonly process the responses instead of having each module use directly the
 * {@link com.terracotta.diagnostic.Diagnostics} class and do their own processing and error handling.
 * <p>
 * This class also setup workarounds for several issues related to the use of string and string parsing in the diagnostic handler.
 *
 * @author Mathieu Carbou
 */
public interface DiagnosticService extends DiagnosticMBeanSupport, Closeable {

  /**
   * Check is the connection to the diagnostic port is still available
   */
  boolean isConnected();

  <T> T getProxy(Class<T> type) throws DiagnosticOperationTimeoutException, DiagnosticOperationExecutionException, DiagnosticConnectionException, DiagnosticOperationUnsupportedException;

  @Override
  void close();

  // ConsistencyManager

  default boolean isBlocked() throws DiagnosticOperationTimeoutException, DiagnosticOperationExecutionException, DiagnosticConnectionException {
    return Boolean.parseBoolean(invoke(MBEAN_CONSISTENCY_MANAGER, "isBlocked"));
  }

  default void allowRequestedTransition() throws DiagnosticOperationTimeoutException, DiagnosticOperationExecutionException, DiagnosticConnectionException {
    invoke(MBEAN_CONSISTENCY_MANAGER, "allowRequestedTransition");
  }

  // L2Dumper

  default void doServerDump() throws DiagnosticOperationTimeoutException, DiagnosticOperationExecutionException, DiagnosticConnectionException {
    try {
      invoke(MBEAN_L2_DUMPER, "doServerDump");
    } catch (DiagnosticOperationTimeoutException e) {
      // do nothing: this is a client timeout. The dump might take longer on server.
    }
  }

  // IPWhitelist

  default void reloadIPWhitelist() throws DiagnosticOperationTimeoutException, DiagnosticOperationExecutionException, DiagnosticConnectionException {
    invoke(MBEAN_IP_WHITELIST, "reload");
  }

  // Shutdown

  default String safeToShutdown() throws DiagnosticOperationTimeoutException, DiagnosticOperationExecutionException, DiagnosticConnectionException {
    return invoke(MBEAN_SHUTDOWN, "safeToShutdown");
  }

  default String safeToShutdown(String shutdownOptions) throws DiagnosticOperationTimeoutException, DiagnosticOperationExecutionException, DiagnosticConnectionException {
    return invokeWithArg(MBEAN_SHUTDOWN, "safeToShutdown", shutdownOptions);
  }

  default String prepareToShutdown() throws DiagnosticOperationTimeoutException, DiagnosticOperationExecutionException, DiagnosticConnectionException {
    return invoke(MBEAN_SHUTDOWN, "prepareToShutdown");
  }

  default String stopIfPassive() throws DiagnosticOperationTimeoutException, DiagnosticOperationExecutionException, DiagnosticConnectionException {
    return invoke(MBEAN_SHUTDOWN, "stopIfPassive");
  }

  default String stopIfActive() throws DiagnosticOperationTimeoutException, DiagnosticOperationExecutionException, DiagnosticConnectionException {
    return invoke(MBEAN_SHUTDOWN, "stopIfActive");
  }

  default String abortShutdown(String reason) throws DiagnosticOperationTimeoutException, DiagnosticOperationExecutionException, DiagnosticConnectionException {
    return invokeWithArg(MBEAN_SHUTDOWN, "abortShutdown", reason);
  }

  default String hardStop() throws DiagnosticOperationTimeoutException, DiagnosticOperationExecutionException, DiagnosticConnectionException {
    return invoke(MBEAN_SHUTDOWN, "hardStop");
  }

  // Backup

  default String prepareForBackup() throws DiagnosticOperationTimeoutException, DiagnosticOperationExecutionException, DiagnosticConnectionException {
    return invoke(MBEAN_BACKUP, "prepareForBackup");
  }

  default String enterOnlineBackupMode() throws DiagnosticOperationTimeoutException, DiagnosticOperationExecutionException, DiagnosticConnectionException {
    return invoke(MBEAN_BACKUP, "enterOnlineBackupMode");
  }

  default String prepareAndEnterOnlineBackupMode() throws DiagnosticOperationTimeoutException, DiagnosticOperationExecutionException, DiagnosticConnectionException {
    return invoke(MBEAN_BACKUP, "prepareAndEnterOnlineBackupMode");
  }

  default String startBackup() throws DiagnosticOperationTimeoutException, DiagnosticOperationExecutionException, DiagnosticConnectionException {
    return invoke(MBEAN_BACKUP, "doBackup");
  }

  default String exitOnlineBackupMode() throws DiagnosticOperationTimeoutException, DiagnosticOperationExecutionException, DiagnosticConnectionException {
    return invoke(MBEAN_BACKUP, "exitOnlineBackupMode");
  }

  default String abortBackup() throws DiagnosticOperationTimeoutException, DiagnosticOperationExecutionException, DiagnosticConnectionException {
    return invoke(MBEAN_BACKUP, "abortBackup");
  }

  default void setUniqueBackupName(String uniqueBackupName) throws DiagnosticOperationTimeoutException, DiagnosticOperationExecutionException, DiagnosticConnectionException {
    set(MBEAN_BACKUP, "UniqueBackupName", uniqueBackupName);
  }

  // TCServerInfoMBean

  default String getTCProperties() throws DiagnosticOperationTimeoutException, DiagnosticOperationExecutionException, DiagnosticConnectionException {
    return invoke(MBEAN_SERVER, "getTCProperties");
  }

  default String getEnvironment() throws DiagnosticOperationTimeoutException, DiagnosticOperationExecutionException, DiagnosticConnectionException {
    return invoke(MBEAN_SERVER, "getEnvironment");
  }

  // DetailedServerState
  LogicalServerState getLogicalServerState() throws DiagnosticOperationTimeoutException, DiagnosticConnectionException;

  // DiagnosticsHandler

  String getInitialState() throws DiagnosticOperationTimeoutException, DiagnosticConnectionException;

  String getClusterState() throws DiagnosticOperationTimeoutException, DiagnosticConnectionException;

  String getConfig() throws DiagnosticOperationTimeoutException, DiagnosticConnectionException;

  String getProcessArguments() throws DiagnosticOperationTimeoutException, DiagnosticConnectionException;

  String getThreadDump() throws DiagnosticOperationTimeoutException, DiagnosticConnectionException;

  void terminateServer() throws DiagnosticOperationTimeoutException, DiagnosticConnectionException;

  void forceTerminateServer() throws DiagnosticOperationTimeoutException, DiagnosticConnectionException;
}
