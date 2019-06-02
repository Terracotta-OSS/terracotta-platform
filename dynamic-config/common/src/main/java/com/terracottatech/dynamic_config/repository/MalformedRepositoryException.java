/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.repository;

public class MalformedRepositoryException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public MalformedRepositoryException(final String message) {
    super(message);
  }

  public MalformedRepositoryException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
