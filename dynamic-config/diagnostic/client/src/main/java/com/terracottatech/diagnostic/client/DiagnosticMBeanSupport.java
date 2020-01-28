/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.diagnostic.client;

/**
 * Support for invoking MBeans directly through Diagnostic port without any codec, directly using the raw diagnostic handler
 *
 * @author Mathieu Carbou
 */
public interface DiagnosticMBeanSupport {

  String get(String name, String attribute) throws DiagnosticOperationTimeoutException, DiagnosticOperationExecutionException, DiagnosticConnectionException;

  void set(String name, String attribute, String arg) throws DiagnosticOperationTimeoutException, DiagnosticOperationExecutionException, DiagnosticConnectionException;

  String invoke(String name, String cmd) throws DiagnosticOperationTimeoutException, DiagnosticOperationExecutionException, DiagnosticConnectionException;

  String invokeWithArg(String name, String cmd, String arg) throws DiagnosticOperationTimeoutException, DiagnosticOperationExecutionException, DiagnosticConnectionException;
}
