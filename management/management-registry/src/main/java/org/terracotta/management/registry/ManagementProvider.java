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

import org.terracotta.management.model.call.Parameter;
import org.terracotta.management.model.capabilities.Capability;
import org.terracotta.management.model.capabilities.context.CapabilityContext;
import org.terracotta.management.model.capabilities.descriptors.Descriptor;
import org.terracotta.management.model.context.Context;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Interface to a provider of management capabilities for certain object class.
 *
 * @author Ludovic Orban
 */
public interface ManagementProvider<T> {

  /**
   * The class of managed objects.
   *
   * @return a class.
   */
  Class<? extends T> getManagedType();

  /**
   * Register an object for management in the current provider.
   *
   * @param managedObject the object to manage.
   * @return true if the object has been registered
   */
  void register(T managedObject);

  /**
   * Unregister a managed object from the current provider.
   *
   * @param managedObject the managed object.
   * @return true if the object has been registered
   */
  void unregister(T managedObject);

  /**
   * Get the set of capability descriptors the current provider provides.
   *
   * @return the set of capability descriptors.
   */
  Collection<? extends Descriptor> getDescriptors();

  /**
   * Get the context that the provided capabilities need to run.
   *
   * @return the context requirements.
   */
  CapabilityContext getCapabilityContext();

  /**
   * @return The full capability of this management provider
   */
  Capability getCapability();

  /**
   * @return The name of this capability
   */
  String getCapabilityName();

  /**
   * Collect statistics, if the provider supports this.
   *
   * @param context        the context.
   * @param statisticNames the statistic names to collect. If empty, collect ALL statistics
   * @return the statistic map, the key being the statistic names.
   */
  Map<String, Number> collectStatistics(Context context, Collection<String> statisticNames);

  /**
   * Call an action, if the provider supports this.
   *
   * @param context    the context.
   * @param methodName the method name.
   * @param parameters the action method's parameters (objects and class names)
   * @param returnType The expected return type
   * @param <V> the expected response type
   * @return the action's return value.
   */
  <V> V callAction(Context context, String methodName, Class<V> returnType, Parameter... parameters) throws ExecutionException;

  /**
   * Call an action, if the provider supports this.
   *
   * @param context    the context.
   * @param methodName the method name.
   * @param parameters the action method's parameters (objects and class names)
   */
  void callAction(Context context, String methodName, Parameter... parameters) throws ExecutionException;

  /**
   * Check wheter this management provider supports the given context
   *
   * @param context The management context
   * @return true if the context is supported by this management provider
   */
  boolean supports(Context context);

  /**
   * Closes the management provider.
   */
  void close();

  Collection<ExposedObject<T>> getExposedObjects();

  ExposedObject<T> findExposedObject(T managedObject);
}
