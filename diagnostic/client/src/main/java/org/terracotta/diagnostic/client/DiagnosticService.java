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
package org.terracotta.diagnostic.client;

import org.terracotta.diagnostic.model.LogicalServerState;

import java.io.Closeable;

import static org.terracotta.diagnostic.common.DiagnosticConstants.MBEAN_CONSISTENCY_MANAGER;
import static org.terracotta.diagnostic.common.DiagnosticConstants.MBEAN_L2_DUMPER;
import static org.terracotta.diagnostic.common.DiagnosticConstants.MBEAN_SERVER;
import static org.terracotta.diagnostic.common.DiagnosticConstants.MBEAN_SHUTDOWN;

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

  // TCServerInfoMBean

  default String getTCProperties() throws DiagnosticOperationTimeoutException, DiagnosticOperationExecutionException, DiagnosticConnectionException {
    return invoke(MBEAN_SERVER, "getTCProperties");
  }

  default String getEnvironment() throws DiagnosticOperationTimeoutException, DiagnosticOperationExecutionException, DiagnosticConnectionException {
    return invoke(MBEAN_SERVER, "getEnvironment");
  }

  default boolean isReconnectWindow() {
    return Boolean.parseBoolean(invoke(MBEAN_SERVER, "isReconnectWindow"));
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
