/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.diagnostic.client;

import com.terracottatech.diagnostic.common.DiagnosticException;

/**
 * Thrown when the diagnostic call returns null after a failure such as EntityException, InterruptedException or MessageCodecException.
 * <p>
 * See DiagnosticEntityClientService for implementation details.
 *
 * @author Mathieu Carbou
 */
public class DiagnosticConnectionException extends DiagnosticException {
  private static final long serialVersionUID = 1L;
}
