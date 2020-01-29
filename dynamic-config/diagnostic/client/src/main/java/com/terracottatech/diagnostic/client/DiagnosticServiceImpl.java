/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.diagnostic.client;

import com.terracotta.diagnostic.Diagnostics;
import com.terracottatech.diagnostic.common.Base64DiagnosticCodec;
import com.terracottatech.diagnostic.common.DiagnosticCodec;
import com.terracottatech.diagnostic.common.DiagnosticRequest;
import com.terracottatech.diagnostic.common.DiagnosticResponse;
import com.terracottatech.diagnostic.common.EmptyParameterDiagnosticCodec;
import com.terracottatech.tools.detailed.state.LogicalServerState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.connection.Connection;

import java.lang.reflect.Proxy;
import java.util.function.Supplier;

import static com.terracottatech.diagnostic.common.DiagnosticConstants.MBEAN_DIAGNOSTIC_REQUEST_HANDLER;
import static com.terracottatech.diagnostic.common.DiagnosticConstants.MESSAGE_INVALID_JMX;
import static com.terracottatech.diagnostic.common.DiagnosticConstants.MESSAGE_NOT_PERMITTED;
import static com.terracottatech.diagnostic.common.DiagnosticConstants.MESSAGE_NULL_RETURN;
import static com.terracottatech.diagnostic.common.DiagnosticConstants.MESSAGE_REQUEST_TIMEOUT;
import static com.terracottatech.diagnostic.common.DiagnosticConstants.MESSAGE_UNKNOWN_COMMAND;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Mathieu Carbou
 */
class DiagnosticServiceImpl implements DiagnosticService {

  private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticServiceImpl.class);

  private final Connection connection;
  private final Diagnostics delegate;
  private final DiagnosticCodec<String> codec;

  DiagnosticServiceImpl(Connection connection, com.terracotta.diagnostic.Diagnostics delegate, DiagnosticCodec<?> codec) {
    this.connection = requireNonNull(connection);
    this.delegate = requireNonNull(delegate);
    // we need to ensure the JMX parameter contains no space at all because the DiagnosticsHandler is poorly written
    // and it is parsing parameters by splitting the string using space character
    this.codec = new EmptyParameterDiagnosticCodec()
        .around(new Base64DiagnosticCodec())
        .around(codec);
  }

  // Diagnostics

  @Override
  public boolean isConnected() {
    try {
      getState();
      return true;
    } catch (DiagnosticOperationNotAllowedException | DiagnosticOperationUnsupportedException | DiagnosticOperationExecutionException e) {
      // should never happen for #getState()
      return true;
    } catch (DiagnosticOperationTimeoutException | DiagnosticConnectionException e) {
      return false;
    }
  }

  @Override
  public <T> T getProxy(Class<T> type) throws DiagnosticOperationTimeoutException, DiagnosticOperationExecutionException, DiagnosticConnectionException, DiagnosticOperationUnsupportedException {
    requireNonNull(type);
    if (!type.isInterface()) {
      throw new IllegalArgumentException("Interface expected");
    }
    if (!Boolean.parseBoolean(invokeWithArg(MBEAN_DIAGNOSTIC_REQUEST_HANDLER, "hasServiceInterface", type.getName()))) {
      throw new DiagnosticOperationUnsupportedException(type.getName());
    }
    return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
      if (method.getDeclaringClass() == Object.class) {
        switch (method.getName()) {
          case "equals":
            return proxy == args[0];
          case "hashCode":
            return type.hashCode();
          case "toString":
            return format("DiagnosticServiceProxy(%s)", type.getName());
          default:
            break;
        }
      }
      return invokeEncoded(type, method.getName(), method.getReturnType(), args);
    }));
  }

  @Override
  public void close() {
    try {
      delegate.close();
    } catch (Exception e) {
      LOGGER.debug("Failed to close diagnostic entity", e);
    }

    try {
      connection.close();
    } catch (Exception e) {
      LOGGER.debug("Failed to close diagnostic connection", e);
    }
  }

  // DiagnosticMBeanSupport

  @Override
  public String get(String name, String attribute) throws DiagnosticOperationTimeoutException, DiagnosticOperationExecutionException, DiagnosticConnectionException {
    return execute(() -> delegate.get(name, attribute));
  }

  @Override
  public void set(String name, String attribute, String arg) throws DiagnosticOperationTimeoutException, DiagnosticOperationExecutionException, DiagnosticConnectionException {
    execute(() -> delegate.set(name, attribute, arg));
  }

  @Override
  public String invoke(String name, String cmd) throws DiagnosticOperationTimeoutException, DiagnosticOperationExecutionException, DiagnosticConnectionException {
    return execute(() -> delegate.invoke(name, cmd));
  }

  @Override
  public String invokeWithArg(String name, String cmd, String arg) throws DiagnosticOperationTimeoutException, DiagnosticOperationExecutionException, DiagnosticConnectionException {
    return execute(() -> delegate.invokeWithArg(name, cmd, arg));
  }

  // DetailedServerState

  @Override
  public LogicalServerState getLogicalServerState() throws DiagnosticOperationTimeoutException, DiagnosticConnectionException {
    try {
      return LogicalServerState.parse(invoke("DetailedServerState", "getDetailedServerState"));
    } catch (DiagnosticOperationUnsupportedException | DiagnosticOperationExecutionException ignored) {
      // maybe we connect to an old version, 10.2 for example, that does not have this MBean
      // in this case, let's try the original Server state Mbean
    }

    String state = LogicalServerState.UNKNOWN.name();
    try {
      state = getState();
    } catch (DiagnosticOperationUnsupportedException | DiagnosticOperationExecutionException ignored) {
      // should never occur for getState()
    }

    boolean blocked = false;
    try {
      blocked = isBlocked();
    } catch (DiagnosticOperationUnsupportedException | DiagnosticOperationExecutionException e2) {
      // in case ConsistencyManager is not there
    }

    return LogicalServerState.from(state, false, blocked);
  }

  // DiagnosticsHandler

  @Override
  public String getInitialState() throws DiagnosticOperationTimeoutException, DiagnosticConnectionException {return execute(delegate::getInitialState);}

  @Override
  public String getClusterState() throws DiagnosticOperationTimeoutException, DiagnosticConnectionException {return execute(delegate::getClusterState);}

  @Override
  public String getConfig() throws DiagnosticOperationTimeoutException, DiagnosticConnectionException {return execute(delegate::getConfig);}

  @Override
  public String getProcessArguments() throws DiagnosticOperationTimeoutException, DiagnosticConnectionException {return execute(delegate::getProcessArguments);}

  @Override
  public String getThreadDump() throws DiagnosticOperationTimeoutException, DiagnosticConnectionException {return execute(delegate::getThreadDump);}

  @Override
  public void terminateServer() throws DiagnosticOperationTimeoutException, DiagnosticConnectionException {execute(true, delegate::terminateServer);}

  @Override
  public void forceTerminateServer() throws DiagnosticOperationTimeoutException, DiagnosticConnectionException {execute(true, delegate::forceTerminateServer);}

  private String getState() throws DiagnosticOperationTimeoutException, DiagnosticConnectionException {
    return execute(delegate::getState);
  }

  @SuppressWarnings("unchecked")
  private <T> T invokeEncoded(Class<?> serviceType, String methodName, Class<T> returnType, Object... args) throws DiagnosticOperationTimeoutException, DiagnosticOperationExecutionException, DiagnosticConnectionException, DiagnosticOperationUnsupportedException {
    LOGGER.trace("invokeEncoded({}, {})", serviceType.getName(), methodName);
    DiagnosticRequest request = new DiagnosticRequest(serviceType, methodName, args);
    String encodedReq = codec.serialize(request);
    String encodedRes = invokeWithArg(MBEAN_DIAGNOSTIC_REQUEST_HANDLER, "request", encodedReq);
    DiagnosticResponse<T> response = codec.deserialize(encodedRes, DiagnosticResponse.class);
    response.getError().map(DiagnosticOperationExecutionException::new).ifPresent(e -> {
      throw e;
    });
    return returnType.isPrimitive() ? response.getBody() : returnType.cast(response.getBody());
  }

  private String execute(Supplier<String> execution) throws DiagnosticOperationTimeoutException, DiagnosticOperationExecutionException, DiagnosticConnectionException {
    return execute(false, execution);
  }

  private String execute(boolean allowNull, Supplier<String> execution) throws DiagnosticOperationTimeoutException, DiagnosticOperationExecutionException, DiagnosticConnectionException {
    // JMXSubsystem transforms all null returns to empty strings.
    // null returns are in the case of EntityException, InterruptedException or MessageCodecException,
    // or in the case of terminateServer or forceTerminateServer
    String result = execution.get();
    if (result == null) {
      if (allowNull) {
        return null;
      } else {
        // a failure happened (EntityException, InterruptedException or MessageCodecException)
        throw new DiagnosticConnectionException();
      }
    }
    if (MESSAGE_NULL_RETURN.equals(result)) {
      // convert back to null empty strings
      return null;
    }
    // Handles all the errors from JMXSubsystem and DiagnosticsHandler
    if (MESSAGE_NOT_PERMITTED.equals(result)) {
      throw new DiagnosticOperationNotAllowedException(result);
    }
    if (MESSAGE_REQUEST_TIMEOUT.equals(result)) {
      throw new DiagnosticOperationTimeoutException(result);
    }
    if (MESSAGE_UNKNOWN_COMMAND.equals(result)) {
      throw new DiagnosticOperationUnsupportedException(result);
    }
    if (result.startsWith(MESSAGE_INVALID_JMX)) {
      throw new DiagnosticOperationExecutionException(result);
    }
    return result;
  }

}