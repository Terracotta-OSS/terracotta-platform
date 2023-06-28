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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * @author Mathieu Carbou
 */
public class DiagnosticRequest implements Serializable {

  private static final long serialVersionUID = 1L;

  private final Class<?> serviceInterface;

  private final String methodName;

  private final Object[] arguments;

  // For Json
  private DiagnosticRequest() {
    this(Object.class, "");
  }

  public DiagnosticRequest(Class<?> serviceInterface, String methodName) {
    this(serviceInterface, methodName, new Object[0]);
  }

  public DiagnosticRequest(Class<?> serviceInterface, String methodName, Object... arguments) {
    this.serviceInterface = requireNonNull(serviceInterface);
    this.methodName = requireNonNull(methodName);
    this.arguments = arguments == null ? new Object[0] : arguments;
  }

  public Class<?> getServiceInterface() {
    return serviceInterface;
  }

  public String getMethodName() {
    return methodName;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public Object[] getArguments() {
    return arguments;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("DiagnosticRequest{");
    sb.append("serviceInterface='").append(serviceInterface.getName()).append('\'');
    sb.append(", methodName='").append(methodName).append('\'');
    sb.append('}');
    return sb.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DiagnosticRequest request = (DiagnosticRequest) o;
    return serviceInterface.equals(request.serviceInterface) &&
        methodName.equals(request.methodName) &&
        Arrays.deepEquals(arguments, request.arguments);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(serviceInterface, methodName);
    result = 31 * result + Arrays.deepHashCode(arguments);
    return result;
  }
}
