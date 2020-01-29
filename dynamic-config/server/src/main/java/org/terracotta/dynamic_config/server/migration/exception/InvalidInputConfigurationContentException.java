/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.server.migration.exception;

import org.terracotta.common.struct.Tuple2;

public class InvalidInputConfigurationContentException extends InvalidInputException {
  private static final long serialVersionUID = 1L;

  @SafeVarargs
  public InvalidInputConfigurationContentException(ErrorCode errorCode, final String s, Tuple2<String, String>... params) {
    super(errorCode, s, params);
  }
}