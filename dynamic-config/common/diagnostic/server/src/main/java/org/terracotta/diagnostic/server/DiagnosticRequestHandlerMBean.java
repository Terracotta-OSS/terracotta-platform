/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.diagnostic.server;

import com.tc.management.TerracottaMBean;

/**
 * MBean interface used as a communication layer
 *
 * @author Mathieu Carbou
 */
public interface DiagnosticRequestHandlerMBean extends TerracottaMBean {
  boolean hasServiceInterface(String serviceName);

  String request(String payload);
}
