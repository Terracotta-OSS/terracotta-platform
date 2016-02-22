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
package org.terracotta.management.capabilities;

import org.terracotta.management.Objects;
import org.terracotta.management.capabilities.context.CapabilityContext;
import org.terracotta.management.capabilities.descriptors.Descriptor;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;

/**
 * @author Ludovic Orban
 * @author Mathieu Carbou
 */
public final class ActionsCapability implements Capability, Serializable {

  private String name;
  private Collection<Descriptor> descriptors;
  private CapabilityContext capabilityContext;

  public ActionsCapability(String name, CapabilityContext capabilityContext, Descriptor... descriptors) {
    this(name, capabilityContext, Arrays.asList(descriptors));
  }

  public ActionsCapability(String name, CapabilityContext capabilityContext, Collection<Descriptor> descriptors) {
    this.name = Objects.requireNonNull(name);
    this.descriptors = Objects.requireNonNull(descriptors);
    this.capabilityContext = Objects.requireNonNull(capabilityContext);
  }

  @Override
  public String getName() {
    return name;
  }

  public Collection<Descriptor> getDescriptors() {
    return descriptors;
  }

  @Override
  public CapabilityContext getCapabilityContext() {
    return capabilityContext;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ActionsCapability that = (ActionsCapability) o;

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
