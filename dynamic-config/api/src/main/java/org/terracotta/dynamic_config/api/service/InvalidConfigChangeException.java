/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.api.service;

public class InvalidConfigChangeException extends Exception {
  private static final long serialVersionUID = -3752283156707939955L;

  public InvalidConfigChangeException(String message) {
    super(message);
  }

  public InvalidConfigChangeException(String message, Throwable cause) {
    super(message, cause);
  }
}
