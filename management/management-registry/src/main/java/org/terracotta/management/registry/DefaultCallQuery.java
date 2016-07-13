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
package org.terracotta.management.registry;

import org.terracotta.management.model.call.ContextualReturn;
import org.terracotta.management.model.call.Parameter;
import org.terracotta.management.model.context.Context;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * @author Mathieu Carbou
 */
public class DefaultCallQuery<T extends Serializable> implements CallQuery<T> {

  private final CapabilityManagementSupport capabilityManagement;
  private final String capabilityName;
  private final String methodName;
  private final Parameter[] parameters;
  private final Collection<Context> contexts;
  private final Class<T> returnType;

  public DefaultCallQuery(CapabilityManagementSupport capabilityManagement, String capabilityName, String methodName, Class<T> returnType, Parameter[] parameters, Collection<Context> contexts) {
    this.capabilityManagement = capabilityManagement;
    this.capabilityName = capabilityName;
    this.methodName = methodName;
    this.parameters = parameters;
    this.contexts = Collections.unmodifiableCollection(new ArrayList<Context>(contexts));
    this.returnType = returnType;
  }

  @Override
  public Class<T> getReturnType() {
    return returnType;
  }

  @Override
  public String getCapabilityName() {
    return capabilityName;
  }

  @Override
  public Collection<Context> getContexts() {
    return contexts;
  }

  @Override
  public String getMethodName() {
    return methodName;
  }

  @Override
  public Parameter[] getParameters() {
    return parameters;
  }

  @Override
  public ResultSet<ContextualReturn<T>> execute() {
    Map<Context, ContextualReturn<T>> contextualResults = new LinkedHashMap<Context, ContextualReturn<T>>(contexts.size());
    Collection<ManagementProvider<?>> managementProviders = capabilityManagement.getManagementProvidersByCapability(capabilityName);

    if (managementProviders.isEmpty()) {
      throw new IllegalArgumentException("Bad capability: " + capabilityName);
    }

    for (Context context : contexts) {
      ContextualReturn<T> result = ContextualReturn.notExecuted(capabilityName, context, methodName);
      for (ManagementProvider<?> managementProvider : managementProviders) {
        if (managementProvider.supports(context)) {
          // just suppose there is only one manager handling calls - should be
          try {
            result = ContextualReturn.of(capabilityName, context, methodName, managementProvider.callAction(context, methodName, returnType, parameters));
          } catch (ExecutionException e) {
            result = ContextualReturn.error(capabilityName, context, methodName, e);
          }
          break;
        }
      }
      contextualResults.put(context, result);
    }

    return new DefaultResultSet<ContextualReturn<T>>(contextualResults);
  }

}
