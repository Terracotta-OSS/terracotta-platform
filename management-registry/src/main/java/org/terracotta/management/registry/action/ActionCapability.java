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
package org.terracotta.management.registry.action;

import org.terracotta.management.capabilities.Capability;
import org.terracotta.management.capabilities.context.CapabilityContext;
import org.terracotta.management.capabilities.descriptors.Descriptor;

import java.util.Collection;

/**
 * @author Ludovic Orban
 */
public class ActionCapability implements Capability {

  private final String name;
  private final Collection<Descriptor> descriptors;
  private final CapabilityContext capabilityContext;

  public ActionCapability(String name, Collection<Descriptor> descriptors, CapabilityContext capabilityContext) {
    this.name = name;
    this.descriptors = descriptors;
    this.capabilityContext = capabilityContext;
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
}
