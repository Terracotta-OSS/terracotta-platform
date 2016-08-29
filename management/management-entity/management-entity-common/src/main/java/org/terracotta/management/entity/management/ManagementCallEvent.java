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
package org.terracotta.management.entity.management;

import org.terracotta.management.model.Objects;
import org.terracotta.management.model.call.Parameter;
import org.terracotta.management.model.cluster.ClientIdentifier;
import org.terracotta.management.model.context.Context;

import java.io.Serializable;
import java.util.Arrays;

/**
 * @author Mathieu Carbou
 */
public class ManagementCallEvent extends ManagementEvent implements Serializable {

  private final Context context;
  private final String capabilityName;
  private final String methodName;
  private final Class<?> returnType;
  private final Parameter[] parameters;

  public ManagementCallEvent(String id, ClientIdentifier from, Context context, String capabilityName, String methodName, Class<?> returnType, Parameter[] parameters) {
    super(id, from);
    this.context = Objects.requireNonNull(context);
    this.capabilityName = Objects.requireNonNull(capabilityName);
    this.methodName = Objects.requireNonNull(methodName);
    this.returnType = Objects.requireNonNull(returnType);
    this.parameters = Objects.requireNonNull(parameters);
  }

  public String getCapabilityName() {
    return capabilityName;
  }

  public Context getContext() {
    return context;
  }

  public String getMethodName() {
    return methodName;
  }

  public Parameter[] getParameters() {
    return parameters;
  }

  public Class<?> getReturnType() {
    return returnType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;

    ManagementCallEvent that = (ManagementCallEvent) o;

    if (!context.equals(that.context)) return false;
    if (!capabilityName.equals(that.capabilityName)) return false;
    if (!methodName.equals(that.methodName)) return false;
    if (!returnType.equals(that.returnType)) return false;
    // Probably incorrect - comparing Object[] arrays with Arrays.equals
    return Arrays.equals(parameters, that.parameters);

  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + context.hashCode();
    result = 31 * result + capabilityName.hashCode();
    result = 31 * result + methodName.hashCode();
    result = 31 * result + returnType.hashCode();
    result = 31 * result + Arrays.hashCode(parameters);
    return result;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("ManagementCallEvent{");
    sb.append("id='").append(getId()).append('\'');
    sb.append(", from=").append(getFrom());
    sb.append(", context=").append(context);
    sb.append(", capabilityName='").append(capabilityName).append('\'');
    sb.append(", methodName='").append(methodName).append('\'');
    sb.append(", returnType=").append(returnType);
    sb.append(", parameters=").append(Arrays.toString(parameters));
    sb.append('}');
    return sb.toString();
  }
}
