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
import org.terracotta.management.model.capabilities.DefaultCapability;
import org.terracotta.management.model.capabilities.context.CapabilityContext;
import org.terracotta.management.model.capabilities.descriptors.Descriptor;
import org.terracotta.management.model.context.Context;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class ManagementProviderAdapter<T> implements ManagementProvider<T> {

  private final String name;
  private final Class<? extends T> managedType;

  public ManagementProviderAdapter(String name, Class<? extends T> managedType) {
    this.name = name;
    this.managedType = managedType;
  }

  @Override
  public final String getCapabilityName() {
    return name;
  }

  @Override
  public final Class<? extends T> getManagedType() {
    return managedType;
  }

  @Override
  public void register(Object managedObject) {
  }

  @Override
  public void close() {
  }

  @Override
  public void unregister(Object managedObject) {
  }

  @Override
  public Collection<Descriptor> getDescriptors() {
    return Collections.emptyList();
  }

  @Override
  public CapabilityContext getCapabilityContext() {
    return new CapabilityContext();
  }

  @Override
  public Capability getCapability() {
    return new DefaultCapability(getCapabilityName(), getCapabilityContext());
  }

  @Override
  public Map<String, Number> collectStatistics(Context context, Collection<String> statisticNames) {
    throw new UnsupportedOperationException("Not a statistics provider : " + getCapabilityName());
  }

  @Override
  public void callAction(Context context, String methodName, Parameter... parameters) {
    throw new UnsupportedOperationException("Not an action provider : " + getCapabilityName());
  }

  @Override
  public <V> V callAction(Context context, String methodName, Class<V> returnType, Parameter... parameters) {
    throw new UnsupportedOperationException("Not an action provider : " + getCapabilityName());
  }

  @Override
  public boolean supports(Context context) {
    return false;
  }

  @Override
  public Collection<ExposedObject<T>> getExposedObjects() {
    return Collections.emptyList();
  }

  @Override
  public ExposedObject<T> findExposedObject(T managedObject) {
    return null;
  }
}
