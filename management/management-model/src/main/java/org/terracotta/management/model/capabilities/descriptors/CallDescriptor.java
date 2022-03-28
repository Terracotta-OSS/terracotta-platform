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
package org.terracotta.management.model.capabilities.descriptors;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @author Ludovic Orban
 * @author Mathieu Carbou
 */
public final class CallDescriptor implements Descriptor, Serializable {

  private static final long serialVersionUID = 1;

  private final String name;
  private final String returnType;
  private final List<Parameter> parameters;

  public CallDescriptor(String name, String returnType, Parameter... parameters) {
    this(name, returnType, Arrays.asList(parameters));
  }

  public CallDescriptor(String name, String returnType, List<Parameter> parameters) {
    this.name = Objects.requireNonNull(name);
    this.returnType = Objects.requireNonNull(returnType);
    this.parameters = Objects.requireNonNull(parameters);
  }

  public String getName() {
    return name;
  }

  public String getReturnType() {
    return returnType;
  }

  public List<Parameter> getParameters() {
    return parameters;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("CallDescriptor{");
    sb.append("name='").append(name).append('\'');
    sb.append(", returnType='").append(returnType).append('\'');
    sb.append(", parameters=").append(parameters);
    sb.append('}');
    return sb.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    CallDescriptor that = (CallDescriptor) o;

    if (!name.equals(that.name)) return false;
    if (!returnType.equals(that.returnType)) return false;
    return parameters.equals(that.parameters);

  }

  @Override
  public int hashCode() {
    int result = name.hashCode();
    result = 31 * result + returnType.hashCode();
    result = 31 * result + parameters.hashCode();
    return result;
  }

  public static final class Parameter implements Serializable {

    private static final long serialVersionUID = 1;

    private final String name;
    private final String type;

    public Parameter(String name, String type) {
      this.name = Objects.requireNonNull(name);
      this.type = Objects.requireNonNull(type);
    }

    public String getName() {
      return name;
    }

    public String getType() {
      return type;
    }

    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder("Parameter{");
      sb.append("name='").append(name).append('\'');
      sb.append(", type='").append(type).append('\'');
      sb.append('}');
      return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      Parameter parameter = (Parameter) o;

      if (!name.equals(parameter.name)) return false;
      return type.equals(parameter.type);

    }

    @Override
    public int hashCode() {
      int result = name.hashCode();
      result = 31 * result + type.hashCode();
      return result;
    }
  }

}
