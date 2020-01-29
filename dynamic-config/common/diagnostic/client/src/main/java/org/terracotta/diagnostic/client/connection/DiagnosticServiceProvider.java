/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.diagnostic.client.connection;

import org.terracotta.connection.ConnectionException;
import org.terracotta.diagnostic.client.DiagnosticService;
import org.terracotta.diagnostic.client.DiagnosticServiceFactory;
import org.terracotta.exception.EntityNotFoundException;
import org.terracotta.exception.EntityNotProvidedException;
import org.terracotta.exception.EntityVersionMismatchException;

import java.net.InetSocketAddress;
import java.time.Duration;

import static java.util.Objects.requireNonNull;

/**
 * @author Mathieu Carbou
 */
public class DiagnosticServiceProvider {

  private final String connectionName;
  private final Duration diagnosticInvokeTimeout;
  private final Duration connectTimeout;
  private final String securityRootDirectory;

  public DiagnosticServiceProvider(String connectionName, Duration connectTimeout, Duration diagnosticInvokeTimeout, String securityRootDirectory) {
    this.connectionName = requireNonNull(connectionName);
    this.diagnosticInvokeTimeout = requireNonNull(diagnosticInvokeTimeout);
    this.connectTimeout = requireNonNull(connectTimeout);
    this.securityRootDirectory = securityRootDirectory;
  }

  public DiagnosticService fetchDiagnosticService(InetSocketAddress address) throws DiagnosticServiceProviderException {
    return fetchDiagnosticService(address, connectTimeout);
  }

  public DiagnosticService fetchDiagnosticService(InetSocketAddress address, Duration connectTimeout) throws DiagnosticServiceProviderException {
    try {
      return DiagnosticServiceFactory.fetch(address, connectionName, connectTimeout, diagnosticInvokeTimeout, securityRootDirectory);
    } catch (EntityNotFoundException | EntityNotProvidedException | EntityVersionMismatchException | ConnectionException e) {
      throw new DiagnosticServiceProviderException(e);
    }
  }
}
