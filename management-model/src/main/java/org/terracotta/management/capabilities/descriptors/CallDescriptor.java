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
package org.terracotta.management.capabilities.descriptors;

import java.util.List;

/**
 * @author Ludovic Orban
 */
public class CallDescriptor implements Descriptor {

  private final String name;
  private final String returnType;
  private final List<Parameter> parameters;

  public CallDescriptor(String name, String returnType, List<Parameter> parameters) {
    this.name = name;
    this.returnType = returnType;
    this.parameters = parameters;
  }

  @Override
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
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    CallDescriptor that = (CallDescriptor) o;

    if (name != null ? !name.equals(that.name) : that.name != null) return false;
    if (returnType != null ? !returnType.equals(that.returnType) : that.returnType != null) return false;
    return !(parameters != null ? !parameters.equals(that.parameters) : that.parameters != null);

  }

  @Override
  public int hashCode() {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + (returnType != null ? returnType.hashCode() : 0);
    result = 31 * result + (parameters != null ? parameters.hashCode() : 0);
    return result;
  }

  public static class Parameter {
    private final String name;
    private final String type;

    public Parameter(String name, String type) {
      this.name = name;
      this.type = type;
    }

    public String getName() {
      return name;
    }

    public String getType() {
      return type;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      Parameter parameter = (Parameter) o;

      if (name != null ? !name.equals(parameter.name) : parameter.name != null) return false;
      return !(type != null ? !type.equals(parameter.type) : parameter.type != null);

    }

    @Override
    public int hashCode() {
      int result = name != null ? name.hashCode() : 0;
      result = 31 * result + (type != null ? type.hashCode() : 0);
      return result;
    }
  }

}
