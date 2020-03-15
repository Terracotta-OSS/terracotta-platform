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

import com.tc.classloader.CommonComponent;
import org.terracotta.management.model.capabilities.Capability;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

/**
 * @author Mathieu Carbou
 */
@CommonComponent
public class CombiningCapabilityManagementSupport implements CapabilityManagementSupport {

  private final CapabilityManagementSupport[] capabilityManagementSupports;

  public CombiningCapabilityManagementSupport(CapabilityManagementSupport... capabilityManagementSupports) {
    Objects.requireNonNull(capabilityManagementSupports);
    for (CapabilityManagementSupport capabilityManagementSupport : capabilityManagementSupports) {
      Objects.requireNonNull(capabilityManagementSupport);
    }
    this.capabilityManagementSupports = capabilityManagementSupports;
  }

  @Override
  public CapabilityManagement withCapability(String capabilityName) {
    return new DefaultCapabilityManagement(this, capabilityName);
  }

  @Override
  public Collection<ManagementProvider<?>> getManagementProvidersByCapability(String capabilityName) {
    ArrayList<ManagementProvider<?>> list = new ArrayList<ManagementProvider<?>>();
    for (CapabilityManagementSupport capabilityManagementSupport : capabilityManagementSupports) {
      list.addAll(capabilityManagementSupport.getManagementProvidersByCapability(capabilityName));
    }
    return list;
  }

  @Override
  public Collection<? extends Capability> getCapabilities() {
    ArrayList<Capability> list = new ArrayList<Capability>();
    for (CapabilityManagementSupport capabilityManagementSupport : capabilityManagementSupports) {
      list.addAll(capabilityManagementSupport.getCapabilities());
    }
    return list;
  }

  @Override
  public Collection<String> getCapabilityNames() {
    ArrayList<String> list = new ArrayList<String>();
    for (CapabilityManagementSupport capabilityManagementSupport : capabilityManagementSupports) {
      list.addAll(capabilityManagementSupport.getCapabilityNames());
    }
    return list;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("CombiningCapabilityManagementSupport{");
    sb.append("capabilityManagementSupports=").append(Arrays.toString(capabilityManagementSupports));
    sb.append('}');
    return sb.toString();
  }
}
