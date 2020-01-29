/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.diagnostic.common;

/**
 * @author Mathieu Carbou
 */
public class DiagnosticException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public DiagnosticException() {
  }

  public DiagnosticException(String message) {
    super(message);
  }

  public DiagnosticException(Throwable cause) {
    super(cause);
  }
}
