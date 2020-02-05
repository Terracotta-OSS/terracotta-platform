/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.diagnostic.client;

import org.terracotta.diagnostic.common.DiagnosticException;

/**
 * Thrown when an operation is not permitted.
 * <p>
 * See: DiagnosticsHandler
 *
 * @author Mathieu Carbou
 */
public class DiagnosticOperationNotAllowedException extends DiagnosticException {
  private static final long serialVersionUID = 1L;

  public DiagnosticOperationNotAllowedException(String message) {
    super(message);
  }
}