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
package org.terracotta.management.model.cluster;

import org.terracotta.management.model.capabilities.Capability;
import org.terracotta.management.model.capabilities.context.CapabilityContext;
import org.terracotta.management.model.capabilities.descriptors.CallDescriptor;
import org.terracotta.management.model.capabilities.descriptors.Descriptor;
import org.terracotta.management.model.capabilities.descriptors.Settings;
import org.terracotta.management.model.capabilities.descriptors.StatisticDescriptor;
import org.terracotta.management.model.context.ContextContainer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Mathieu Carbou
 */
public final class ManagementRegistry implements Serializable {

  private static final long serialVersionUID = 2;

  private final ContextContainer contextContainer;
  private final Collection<Capability> capabilities = new ArrayList<>();

  private ManagementRegistry(ContextContainer contextContainer) {
    this.contextContainer = Objects.requireNonNull(contextContainer);
  }

  public ManagementRegistry setCapabilities(Collection<Capability> capabilities) {
    this.capabilities.clear();
    capabilities.forEach(this::addCapability);
    return this;
  }

  public ManagementRegistry setCapabilities(Capability... capabilities) {
    return setCapabilities(Arrays.asList(capabilities));
  }

  public ManagementRegistry addCapability(Capability capability) {
    this.capabilities.add(capability);
    return this;
  }

  public ManagementRegistry addCapabilities(Capability... capabilities) {
    for (Capability capability : capabilities) {
      addCapability(capability);
    }
    return this;
  }

  public Collection<Capability> getCapabilities() {
    return capabilities;
  }

  public Optional<Capability> getCapability(String capabilityName) {
    return capabilities.stream().filter(capability -> capability.getName().equals(capabilityName)).findFirst();
  }

  public ContextContainer getContextContainer() {
    return contextContainer;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ManagementRegistry that = (ManagementRegistry) o;

    if (!contextContainer.equals(that.contextContainer)) return false;
    return capabilities.equals(that.capabilities);

  }

  @Override
  public int hashCode() {
    int result = contextContainer.hashCode();
    result = 31 * result + capabilities.hashCode();
    return result;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("ManagementRegistry{");
    sb.append("contextContainer=").append(contextContainer);
    sb.append(", capabilities=").append(capabilities.size());
    sb.append('}');
    return sb.toString();
  }

  public Map<String, Object> toMap() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("contextContainer", toMap(contextContainer));
    map.put("capabilities", this.capabilities.stream().map(ManagementRegistry::toMap).collect(Collectors.toList()));
    return map;
  }

  private static Map<String, Object> toMap(Capability capability) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("name", capability.getName());
    map.put("context", capability.getCapabilityContext().getAttributes().stream().map(ManagementRegistry::toMap).collect(Collectors.toList()));

    List<Map<String, Object>> descriptorList = new ArrayList<>(capability.getDescriptors().size());
    map.put("descriptors", descriptorList);
    for (Descriptor o : capability.getDescriptors()) {
      if (o instanceof CallDescriptor) {
        descriptorList.add(toMap((CallDescriptor) o));
      } else if (o instanceof StatisticDescriptor) {
        descriptorList.add(toMap((StatisticDescriptor) o));
      } else if (o instanceof Settings) {
        descriptorList.add(toMap((Settings) o));
      } else {
        descriptorList.add(toMap(o));
      }
    }
    return map;
  }

  private static Map<String, Object> toMap(CapabilityContext.Attribute attribute) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("name", attribute.getName());
    map.put("required", attribute.isRequired());
    return map;
  }

  private static Map<String, Object> toMap(StatisticDescriptor descriptor) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("name", descriptor.getName());
    map.put("type", descriptor.getType());
    return map;
  }

  private static Map<String, Object> toMap(CallDescriptor descriptor) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("name", descriptor.getName());
    map.put("returnType", descriptor.getReturnType());
    map.put("parameters", descriptor.getParameters().stream().map(ManagementRegistry::toMap).collect(Collectors.toList()));
    return map;
  }

  private static Map<String, Object> toMap(Settings descriptor) {
    // settings descriptor is already a map of simple types
    return descriptor;
  }

  private static Map<String, Object> toMap(Descriptor descriptor) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("type", descriptor.getClass().getName());
    return map;
  }

  private static Map<String, Object> toMap(CallDescriptor.Parameter parameter) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("name", parameter.getName());
    map.put("type", parameter.getType());
    return map;
  }

  private static Map<String, Object> toMap(ContextContainer contextContainer) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put(contextContainer.getName(), contextContainer.getValue());
    map.put("subContexts", contextContainer.getSubContexts().stream().map(ManagementRegistry::toMap).collect(Collectors.toList()));
    return map;
  }

  public static ManagementRegistry create(ContextContainer contextContainer) {
    return new ManagementRegistry(contextContainer);
  }

}
