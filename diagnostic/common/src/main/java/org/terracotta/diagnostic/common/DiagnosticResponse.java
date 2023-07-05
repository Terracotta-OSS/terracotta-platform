/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.diagnostic.common;

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

  private final T body;

  private final String errorType;
  private final String errorMessage;
  private final String errorStack;

  // For Json
  private DiagnosticResponse() {
    this(null);
  }

  public DiagnosticResponse(T body) {
    this(body, null);
  }

  public DiagnosticResponse(T body, Throwable error) {
    this(body, error == null ? null : error.getClass().getName(), error == null ? null : error.getMessage(), error == null ? null : stackTrace(error));
  }

  protected DiagnosticResponse(T body, String errorType, String errorMessage, String errorStack) {
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

  public Optional<String> getError() {
    return getErrorType().map(type -> type + getErrorMessage().map(msg -> ": " + msg).orElse(""));
  }

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
