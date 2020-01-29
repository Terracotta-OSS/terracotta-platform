/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.diagnostic.server;

import java.io.Closeable;

/**
 * @author Mathieu Carbou
 */
public interface DiagnosticServicesRegistration<T> extends Closeable {

  default boolean registerMBean(String name) {
    return DiagnosticServices.registerMBean(name, getServiceInterface());
  }

  Class<T> getServiceInterface();

  @Override
  default void close() {
    DiagnosticServices.unregister(getServiceInterface());
  }

}
