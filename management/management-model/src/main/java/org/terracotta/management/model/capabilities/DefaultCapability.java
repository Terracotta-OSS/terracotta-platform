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
package org.terracotta.management.model.capabilities;

import org.terracotta.management.model.Objects;
import org.terracotta.management.model.capabilities.context.CapabilityContext;
import org.terracotta.management.model.capabilities.descriptors.Descriptor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * @author Ludovic Orban
 * @author Mathieu Carbou
 */
public final class DefaultCapability implements Capability, Serializable {

  private static final long serialVersionUID = 1;

  private final String name;
  private final Collection<? extends Descriptor> descriptors;
  private final CapabilityContext capabilityContext;

  public DefaultCapability(String name, CapabilityContext capabilityContext, Descriptor... descriptors) {
    this(name, capabilityContext, Arrays.asList(descriptors));
  }

  public DefaultCapability(String name, CapabilityContext capabilityContext, Collection<? extends Descriptor> descriptors) {
    this.name = Objects.requireNonNull(name);
    this.descriptors = Objects.requireNonNull(descriptors);
    this.capabilityContext = Objects.requireNonNull(capabilityContext);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Collection<? extends Descriptor> getDescriptors() {
    return descriptors;
  }

  @Override
  public <T extends Descriptor> Collection<T> getDescriptors(Class<T> descriptorType) {
    Collection<T> list = new ArrayList<T>();
    for (Descriptor descriptor : descriptors) {
      if (descriptorType.isInstance(descriptor)) {
        list.add(descriptorType.cast(descriptor));
      }
    }
    return list;
  }

  @Override
  public CapabilityContext getCapabilityContext() {
    return capabilityContext;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("Capability{");
    sb.append("name='").append(name).append('\'');
    sb.append(", context=").append(capabilityContext);
    sb.append(", descriptors=").append(descriptors);
    sb.append('}');
    return sb.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    DefaultCapability that = (DefaultCapability) o;

    if (!name.equals(that.name)) return false;
    if (!descriptors.equals(that.descriptors)) return false;
    return capabilityContext.equals(that.capabilityContext);

  }

  @Override
  public int hashCode() {
    int result = name.hashCode();
    result = 31 * result + descriptors.hashCode();
    result = 31 * result + capabilityContext.hashCode();
    return result;
  }

}
