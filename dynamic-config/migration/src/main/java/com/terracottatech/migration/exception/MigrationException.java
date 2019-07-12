/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.migration.exception;

import com.terracottatech.utilities.Tuple2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class MigrationException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final Map<String, List<String>> parameters = new HashMap<>();
  private final ErrorCode errorCode;

  @SafeVarargs
  public MigrationException(ErrorCode errorCode, final String s, Tuple2<String, String>... params) {
    super(s);
    this.errorCode = errorCode;
    if (params != null) {
      @SuppressWarnings("varargs")
      Stream<Tuple2<String, String>> paramStream = Arrays.stream(params);
      paramStream.forEach(param -> {
        parameters.compute(param.getT1(), (key, value) -> {
          List<String> retValue;
          if (value == null) {
            retValue = new ArrayList<>();
          } else {
            retValue = value;
          }
          retValue.add(param.getT2());
          return retValue;
        });
      });
    }
  }

  public Map<String, List<String>> getParameters() {
    return Collections.unmodifiableMap(parameters);
  }

  public ErrorCode getErrorCode() {
    return errorCode;
  }
}