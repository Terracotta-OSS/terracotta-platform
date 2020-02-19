/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.diagnostic.client;

import com.terracotta.diagnostic.Diagnostics;
import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionException;
import org.terracotta.connection.ConnectionFactory;
import org.terracotta.connection.ConnectionPropertyNames;
import org.terracotta.connection.ConnectionService;
import org.terracotta.connection.entity.EntityRef;
import org.terracotta.diagnostic.common.DiagnosticCodec;
import org.terracotta.diagnostic.common.JsonDiagnosticCodec;
import org.terracotta.exception.EntityException;
import org.terracotta.exception.EntityNotFoundException;
import org.terracotta.exception.EntityNotProvidedException;
import org.terracotta.exception.EntityVersionMismatchException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

/**
 * @author Mathieu Carbou
 */
public class DiagnosticServiceFactory {

  private static final String SECURITY_ROOT_DIRECTORY = "security.root.directory";

  public static DiagnosticService fetch(InetSocketAddress nodeAddress,
                                        String connectionName,
                                        Duration connectTimeout,
                                        Duration diagnosticInvokeTimeout,
                                        String securityRootDirectory) throws ConnectionException {
    Properties properties = buildProperties(connectionName, connectTimeout, securityRootDirectory);
    Connection connection = ConnectionFactory.connect(Collections.singletonList(nodeAddress), properties);
    try {
      return fetch(connection, diagnosticInvokeTimeout);
    } catch (EntityException e) {
      try {
        connection.close();
      } catch (IOException ignored) {
      }
      // we decide to consider any entity exception as connection exception because the diagnostic entity should always be there and
      // the caller anyway has no way to properly react to these catch exception. Only ConnectionException is relevant.
      throw new ConnectionException(e);
    }
  }

  public static DiagnosticService fetch(ConnectionService connectionService,
                                        InetSocketAddress nodeAddress,
                                        String connectionName,
                                        Duration connectionTimeout,
                                        Duration diagnosticInvokeTimeout,
                                        String securityRootDirectory) throws ConnectionException {
    Properties properties = buildProperties(connectionName, connectionTimeout, securityRootDirectory);
    Connection connection = connectionService.connect(Collections.singletonList(nodeAddress), properties);
    try {
      return fetch(connection, diagnosticInvokeTimeout);
    } catch (EntityException e) {
      try {
        connection.close();
      } catch (IOException ignored) {
      }
      // we decide to consider any entity exception as connection exception because the diagnostic entity should always be there and
      // the caller anyway has no way to properly react to these catch exception. Only ConnectionException is relevant.
      throw new ConnectionException(e);
    }

  }

  public static DiagnosticService getDiagnosticService(Connection connection, Diagnostics delegate) {
    // We could default to the JavaDiagnosticCodec also, or a runnel codec if we want to get rid of Json and only do serialization.
    // The codec needs to be the same on client-side and server-side of course.
    return getDiagnosticService(connection, delegate, new JsonDiagnosticCodec());
  }

  public static DiagnosticService getDiagnosticService(Connection connection, Diagnostics delegate, DiagnosticCodec<?> codec) {
    return new DiagnosticServiceImpl(connection, delegate, codec);
  }

  private static DiagnosticService fetch(Connection connection, Duration diagnosticInvokeTimeout)
      throws EntityNotProvidedException, EntityVersionMismatchException, EntityNotFoundException {
    EntityRef<com.terracotta.diagnostic.Diagnostics, Object, Properties> ref = connection.getEntityRef(com.terracotta.diagnostic.Diagnostics.class, 1, "root");
    Properties properties = new Properties();
    properties.setProperty("request.timeout", String.valueOf(diagnosticInvokeTimeout.toMillis()));
    Diagnostics delegate = ref.fetchEntity(properties);
    return getDiagnosticService(connection, delegate);
  }

  private static Properties buildProperties(String connectionName, Duration connectionTimeout, String securityRootDirectory) {
    Properties properties = new Properties();
    properties.setProperty(ConnectionPropertyNames.CONNECTION_TIMEOUT, String.valueOf(connectionTimeout.toMillis()));
    properties.setProperty(ConnectionPropertyNames.CONNECTION_NAME, connectionName);
    properties.setProperty(ConnectionPropertyNames.CONNECTION_TYPE, "diagnostic");
    if (securityRootDirectory != null) {
      properties.setProperty(SECURITY_ROOT_DIRECTORY, securityRootDirectory);
    }
    return properties;
  }

}
