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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.terracotta.management.model.call.ContextualReturn;
import org.terracotta.management.model.call.Parameter;
import org.terracotta.management.model.context.Context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

/**
 * @author Mathieu Carbou
 */
public class DefaultCallQuery<T> implements CallQuery<T> {

  private final CapabilityManagementSupport capabilityManagement;
  private final String capabilityName;
  private final String methodName;
  private final Parameter[] parameters;
  private final Collection<Context> contexts;
  private final Class<T> returnType;

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public DefaultCallQuery(CapabilityManagementSupport capabilityManagement, String capabilityName, String methodName, Class<T> returnType, Parameter[] parameters, Collection<Context> contexts) {
    this.capabilityManagement = Objects.requireNonNull(capabilityManagement);
    this.capabilityName = Objects.requireNonNull(capabilityName);
    this.methodName = Objects.requireNonNull(methodName);
    this.parameters = Objects.requireNonNull(parameters);
    this.contexts = Collections.unmodifiableCollection(new ArrayList<Context>(Objects.requireNonNull(contexts)));
    this.returnType = Objects.requireNonNull(returnType);

    if(contexts.isEmpty()) {
      throw new IllegalArgumentException("You did not specify any context to execute the management call onto");
    }
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
    return Arrays.copyOf(parameters, parameters.length);
  }

  @Override
  public ResultSet<ContextualReturn<T>> execute() {
    Map<Context, ContextualReturn<T>> contextualResults = new LinkedHashMap<Context, ContextualReturn<T>>(contexts.size());
    Collection<ManagementProvider<?>> managementProviders = capabilityManagement.getManagementProvidersByCapability(capabilityName);

    for (Context context : contexts) {
      ContextualReturn<T> result = ContextualReturn.notExecuted(capabilityName, context, methodName);
      for (ManagementProvider<?> managementProvider : managementProviders) {
        if (managementProvider.supports(context)) {
          // just suppose there is only one manager handling calls - should be
          try {
            result = ContextualReturn.of(capabilityName, context, methodName, managementProvider.callAction(context, methodName, returnType, parameters));
          } catch (ExecutionException e) {
            result = ContextualReturn.error(capabilityName, context, methodName, e);
          } catch (Exception e) {
            result = ContextualReturn.error(capabilityName, context, methodName, new ExecutionException(e.getMessage(), e));
          }
          break;
        }
      }
      contextualResults.put(context, result);
    }

    return new DefaultResultSet<ContextualReturn<T>>(contextualResults);
  }

}
