/**
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
package org.terracotta.voltron.management.common.providers;

import org.terracotta.management.capabilities.Capability;
import org.terracotta.management.capabilities.context.CapabilityContext;
import org.terracotta.management.capabilities.descriptors.Descriptor;

import java.util.Collection;

/**
 * Required interface that every managed entity MUST implement for all its managed object
 * type(s) in order to connect the managed object to external management system(s) through
 * management entity.
 * <p>
 * Typically, it is assumed that there will be one management provider for each capability
 * per managed object type.
 *
 * @param <O> the managed object
 *
 * @author RKAV
 */
public interface ManagementProvider<O> {

  /**
   * The class of managed objects.
   *
   * @return the managed object type.
   */
  Class<O> managedType();

  /**
   * Get the set of capability descriptors the current provider provides.
   *
   * @return the set of capability descriptors.
   */
  Collection<Descriptor> getDescriptors();

  /**
   * Get the context that the provided capabilities need to run.
   *
   * @return the context requirements.
   */
  CapabilityContext getCapabilityContext();

  /**
   * Get the capability that this management provider provides.
   *
   * @return The full capability of this management provider
   */
  Capability getCapability();

  /**
   * Capability name.
   *
   * @return The name of this capability
   */
  String getCapabilityName();
}
