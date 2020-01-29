/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.diagnostic.client;

import org.terracotta.diagnostic.common.DiagnosticException;

/**
 * Thrown when the diagnostic call times out as per the "request.timeoutMessage" property which defaults to "Request Timeout"
 * <p>
 * See: DiagnosticEntityClientService
 *
 * @author Mathieu Carbou
 */
public class DiagnosticOperationTimeoutException extends DiagnosticException {
  private static final long serialVersionUID = 1L;

  public DiagnosticOperationTimeoutException(String message) {
    super(message);
  }
}
