/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.diagnostic.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Mathieu Carbou
 */
public class DiagnosticResponse<T> implements Serializable {

  private static final long serialVersionUID = 1L;

  // Note: using "@class" here is fine since we control both end of input and output json plus the communication channel
  @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
  private final T body;

  private final String errorType;
  private final String errorMessage;
  private final String errorStack;

  public DiagnosticResponse(T body) {
    this(body, null);
  }

  public DiagnosticResponse(T body, Throwable error) {
    this(body, error == null ? null : error.getClass().getName(), error == null ? null : error.getMessage(), error == null ? null : stackTrace(error));
  }

  @JsonCreator
  DiagnosticResponse(@JsonProperty("body") T body,
                     @JsonProperty("errorType") String errorType,
                     @JsonProperty("errorMessage") String errorMessage,
                     @JsonProperty("errorStack") String errorStack) {
    this.body = body;
    this.errorType = errorType;
    this.errorMessage = errorMessage;
    this.errorStack = errorStack;
  }

  public Optional<String> getErrorType() {
    return Optional.ofNullable(errorType);
  }

  public Optional<String> getErrorMessage() {
    return Optional.ofNullable(errorMessage);
  }

  public Optional<String> getErrorStack() {
    return Optional.ofNullable(errorStack);
  }

  @JsonIgnore
  public Optional<String> getError() {
    return getErrorType().map(type -> type + getErrorMessage().map(msg -> ": " + msg).orElse(""));
  }

  @JsonIgnore
  public boolean hasError() {
    return getErrorType().isPresent();
  }

  public T getBody() {
    return body;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DiagnosticResponse<?> response = (DiagnosticResponse<?>) o;
    return Objects.equals(body, response.body) &&
        Objects.equals(errorType, response.errorType) &&
        Objects.equals(errorStack, response.errorStack);
  }

  @Override
  public int hashCode() {
    return Objects.hash(body, errorType, errorStack);
  }

  private static String stackTrace(Throwable e) {
    StringWriter stack = new StringWriter();
    e.printStackTrace(new PrintWriter(stack));
    return stack.toString();
  }

}
