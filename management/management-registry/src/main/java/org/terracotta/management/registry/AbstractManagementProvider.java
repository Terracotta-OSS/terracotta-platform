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
import org.terracotta.management.model.capabilities.descriptors.StatisticDescriptor;
import org.terracotta.management.model.context.Context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;

/**
 * @author Mathieu Carbou
 */
public abstract class AbstractManagementProvider<T> implements ManagementProvider<T> {

  protected static final Comparator<StatisticDescriptor> STATISTIC_DESCRIPTOR_COMPARATOR = new Comparator<StatisticDescriptor>() {
    @Override
    public int compare(StatisticDescriptor o1, StatisticDescriptor o2) {
      return o1.getName().compareTo(o2.getName());
    }
  };

  private final Queue<ExposedObject<T>> exposedObjects = new ConcurrentLinkedQueue<ExposedObject<T>>();

  private final String capabilityName;
  private final Class<? extends T> managedType;
  private final CapabilityContext capabilityContext;

  public AbstractManagementProvider(Class<? extends T> managedType) {
    this.managedType = managedType;
    this.capabilityName = buildCapabilityName();
    this.capabilityContext = buildCapabilityContext();
  }

  @Override
  public Class<? extends T> getManagedType() {
    return managedType;
  }

  @Override
  public String getCapabilityName() {
    return capabilityName;
  }

  @Override
  public CapabilityContext getCapabilityContext() {
    return capabilityContext;
  }

  @Override
  public Capability getCapability() {
    return new DefaultCapability(getCapabilityName(), getCapabilityContext(), getDescriptors());
  }

  @Override
  public void register(T managedObject) {
    ExposedObject<T> exposedObject = wrap(managedObject);
    this.exposedObjects.add(exposedObject);
  }

  @Override
  public void unregister(T managedObject) {
    for (ExposedObject<T> exposedObject : exposedObjects) {
      if (exposedObject.getTarget().equals(managedObject)) {
        if (this.exposedObjects.remove(exposedObject)) {
          dispose(exposedObject);
          return;
        }
      }
    }
  }

  @Override
  public void close() {
    while (!exposedObjects.isEmpty()) {
      dispose(exposedObjects.poll());
    }
  }

  @Override
  public boolean supports(Context context) {
    return findExposedObject(context) != null;
  }

  @Override
  public Map<String, Number> collectStatistics(Context context, Collection<String> statisticNames) {
    throw new UnsupportedOperationException("Not a statistics provider : " + getCapabilityName());
  }

  @Override
  public void callAction(Context context, String methodName, Parameter... parameters) throws ExecutionException {
    callAction(context, methodName, Object.class, parameters);
  }

  @Override
  public <V> V callAction(Context context, String methodName, Class<V> returnType, Parameter... parameters) throws ExecutionException {
    throw new UnsupportedOperationException("Not an action provider : " + getCapabilityName());
  }

  @SuppressWarnings("unchecked")
  @Override
  public Collection<? extends Descriptor> getDescriptors() {
    // LinkedHashSet to keep ordering because these objects end up in an immutable
    // topology so this is easier for testing to compare with json payloads
    Collection<Descriptor> capabilities = new LinkedHashSet<Descriptor>();
    for (ExposedObject o : exposedObjects) {
      capabilities.addAll(((ExposedObject<T>) o).getDescriptors());
    }
    return capabilities;
  }

  protected String buildCapabilityName() {
    Named named = getClass().getAnnotation(Named.class);
    return named == null ? getClass().getSimpleName() : named.value();
  }

  // first try to find annotation on managedType, which might not be there, in this case tries to find from this subclass
  protected CapabilityContext buildCapabilityContext() {
    Collection<CapabilityContext.Attribute> attrs = new ArrayList<CapabilityContext.Attribute>();
    RequiredContext requiredContext = getManagedType().getAnnotation(RequiredContext.class);
    if (requiredContext == null) {
      requiredContext = getClass().getAnnotation(RequiredContext.class);
    }
    if (requiredContext == null) {
      throw new IllegalStateException("@RequiredContext not found on " + getManagedType().getName() + " or " + getClass().getName());
    }
    for (Named n : requiredContext.value()) {
      attrs.add(new CapabilityContext.Attribute(n.value(), true));
    }
    return new CapabilityContext(attrs);
  }

  protected void dispose(ExposedObject<T> exposedObject) {
  }

  @Override
  public Collection<ExposedObject<T>> getExposedObjects() {
    return exposedObjects;
  }

  protected abstract ExposedObject<T> wrap(T managedObject);

  protected ExposedObject<T> findExposedObject(Context context) {
    if (!contextValid(context)) {
      return null;
    }
    for (ExposedObject<T> exposedObject : exposedObjects) {
      if (context.contains(exposedObject.getContext())) {
        return exposedObject;
      }
    }
    return null;
  }

  @Override
  public ExposedObject<T> findExposedObject(T managedObject) {
    for (ExposedObject<T> exposed : exposedObjects) {
      if (exposed.getTarget().equals(managedObject)) {
        return exposed;
      }
    }
    return null;
  }

  private boolean contextValid(Context context) {
    if (context == null) {
      return false;
    }
    for (CapabilityContext.Attribute attribute : getCapabilityContext().getAttributes()) {
      if (context.get(attribute.getName()) == null) {
        return false;
      }
    }
    return true;
  }

}
