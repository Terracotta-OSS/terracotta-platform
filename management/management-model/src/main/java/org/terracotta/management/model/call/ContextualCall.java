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
package org.terracotta.management.model.call;

import org.terracotta.management.model.Objects;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.context.Contextual;

import java.lang.reflect.Array;
import java.util.Arrays;

/**
 * @author Mathieu Carbou
 */
public class ContextualCall<T> implements Contextual {

  private static final long serialVersionUID = 1;

  private Context context;
  private final String capability;
  private final String methodName;
  private final Class<T> returnType;
  private final Parameter[] parameters;

  public ContextualCall(Context context, String capability, String methodName, Class<T> returnType, Parameter... parameters) {
    this.context = Objects.requireNonNull(context);
    this.capability = Objects.requireNonNull(capability);
    this.methodName = Objects.requireNonNull(methodName);
    this.returnType = Objects.requireNonNull(returnType);
    this.parameters = Objects.requireNonNull(parameters);
  }

  public String getCapability() {
    return capability;
  }

  @Override
  public Context getContext() {
    return context;
  }

  @Override
  public void setContext(Context context) {
    this.context = Objects.requireNonNull(context);
  }

  public String getMethodName() {
    return methodName;
  }

  public Parameter[] getParameters() {
    return Arrays.copyOf(parameters, parameters.length);
  }

  public Class<T> getReturnType() {
    return returnType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;

    ContextualCall<?> that = (ContextualCall<?>) o;

    if (!context.equals(that.context)) return false;
    if (!capability.equals(that.capability)) return false;
    if (!methodName.equals(that.methodName)) return false;
    if (!returnType.equals(that.returnType)) return false;
    // Probably incorrect - comparing Object[] arrays with Arrays.equals
    return Arrays.equals(parameters, that.parameters);

  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + context.hashCode();
    result = 31 * result + capability.hashCode();
    result = 31 * result + methodName.hashCode();
    result = 31 * result + returnType.hashCode();
    result = 31 * result + Arrays.hashCode(parameters);
    return result;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("ContextualCall{");
    sb.append("context=").append(context);
    sb.append(", capability='").append(capability).append('\'');
    sb.append(", methodName='").append(methodName).append('\'');
    sb.append(", returnType=").append(returnType);
    sb.append('}');
    return sb.toString();
  }

}
