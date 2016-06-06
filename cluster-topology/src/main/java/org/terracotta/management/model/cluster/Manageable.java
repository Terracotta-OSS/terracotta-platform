/*
 * Copyright Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.terracotta.management.model.cluster;

import org.terracotta.management.model.capabilities.Capability;
import org.terracotta.management.model.capabilities.StatisticsCapability;
import org.terracotta.management.model.capabilities.context.CapabilityContext;
import org.terracotta.management.model.capabilities.descriptors.CallDescriptor;
import org.terracotta.management.model.capabilities.descriptors.Descriptor;
import org.terracotta.management.model.capabilities.descriptors.StatisticDescriptor;
import org.terracotta.management.model.capabilities.descriptors.StatisticDescriptorCategory;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.context.ContextContainer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Mathieu Carbou
 */
public final class Manageable extends AbstractNode<Node> {

  private static final long serialVersionUID = 1;

  public static final String KEY = "manageableId";
  public static final String TYPE_KEY = "manageableType";
  public static final String NAME_KEY = "manageableName";

  private final String type; // type (service, client entity, server entity, etc)
  private final String name; // type (entity type name)
  private ContextContainer contextContainer; // management registry output
  private Map<String, Capability> capabilities = new LinkedHashMap<>();

  // matches management registry config, or entity id, or service type
  private Manageable(String id, String name, String type) {
    super(id);
    this.type = Objects.requireNonNull(type);
    this.name = Objects.requireNonNull(name);
  }

  public Manageable setCapabilities(Collection<Capability> capabilities) {
    this.capabilities.clear();
    capabilities.forEach(this::addCapability);
    return this;
  }

  public Manageable setCapabilities(Capability... capabilities) {
    return setCapabilities(Arrays.asList(capabilities));
  }

  public Manageable addCapability(Capability capability) {
    this.capabilities.put(capability.getName(), capability);
    return this;
  }

  public Manageable addCapabilities(Capability... capabilities) {
    for (Capability capability : capabilities) {
      addCapability(capability);
    }
    return this;
  }

  public Collection<Capability> getCapabilities() {
    return capabilities.values();
  }

  public Capability findCapability(String id) {
    for (Capability capability : capabilities.values()) {
      if (capability.getName().equals(name)) {
        return capability;
      }
    }
    return null;
  }

  public Manageable setContextContainer(ContextContainer contextContainer) {
    this.contextContainer = contextContainer;
    return this;
  }

  public ContextContainer getContextContainer() {
    return contextContainer;
  }

  public String getType() {
    return type;
  }

  public boolean isType(String type) {
    return this.type.equals(type);
  }

  public String getName() {
    return name;
  }

  @Override
  public Context getContext() {
    Context context = super.getContext()
        .with(NAME_KEY, name)
        .with(TYPE_KEY, type);
    return contextContainer == null ? context : context.with(contextContainer.getName(), contextContainer.getValue());
  }

  @Override
  public void remove() {
    Node parent = getParent();
    if (parent != null && parent instanceof ManageableContainer) {
      ((ManageableContainer) parent).removeManageable(getId());
    }
  }

  public <T extends Node> T getContainer(Class<T> type) {
    return type.cast(getParent());
  }

  public Collection<Context> getAllCapabilityContexts(String capabilityName) {
    Context thisContext = getContext();
    Capability capability = capabilities.get(capabilityName);
    if (capability == null) {
      throw new IllegalArgumentException(capabilityName);
    }

    // get all attributes to make this capability works
    Collection<String> requiredAttributesNames = capability.getCapabilityContext().getRequiredAttributeNames();
    boolean addMainCtx = requiredAttributesNames.contains(contextContainer.getName());

    Collection<Context> contexts = new ArrayList<>(contextContainer.getSubContexts().size());
    for (ContextContainer subCtx : contextContainer.getSubContexts()) {
      if (requiredAttributesNames.contains(subCtx.getName())) {
        Context ctx = thisContext.with(subCtx.getName(), subCtx.getValue());
        if (addMainCtx) {
          ctx = ctx.with(contextContainer.getName(), contextContainer.getValue());
        }
        contexts.add(ctx);
      }
    }

    return contexts;
  }

  @Override
  String getContextKey() {
    return KEY;
  }

  public boolean is(String name, String type) {
    return this.name.equals(name) && this.type.equals(type);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;

    Manageable that = (Manageable) o;

    if (!type.equals(that.type)) return false;
    if (!name.equals(that.name)) return false;
    if (contextContainer != null ? !contextContainer.equals(that.contextContainer) : that.contextContainer != null) return false;
    return capabilities != null ? capabilities.equals(that.capabilities) : that.capabilities == null;

  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + type.hashCode();
    result = 31 * result + name.hashCode();
    result = 31 * result + (contextContainer != null ? contextContainer.hashCode() : 0);
    result = 31 * result + (capabilities != null ? capabilities.hashCode() : 0);
    return result;
  }

  @Override
  public Map<String, Object> toMap() {
    Map<String, Object> map = super.toMap();
    map.put("type", getType());
    map.put("name", getName());
    if (contextContainer != null) {
      map.put("contextContainer", toMap(contextContainer));
    }
    map.put("capabilities", this.capabilities.values().stream().map(Manageable::toMap).collect(Collectors.toList()));
    return map;
  }

  private static Map<String, Object> toMap(Capability capability) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("name", capability.getName());
    map.put("context", capability.getCapabilityContext().getAttributes().stream().map(Manageable::toMap).collect(Collectors.toList()));

    List<Map<String, Object>> descriptorList = new ArrayList<>(capability.getDescriptors().size());
    map.put("descriptors", descriptorList);
    for (Descriptor o : capability.getDescriptors()) {
      if (o instanceof CallDescriptor) {
        descriptorList.add(toMap((CallDescriptor) o));
      } else if (o instanceof StatisticDescriptor) {
        descriptorList.add(toMap((StatisticDescriptor) o));
      } else if (o instanceof StatisticDescriptorCategory) {
        descriptorList.add(toMap((StatisticDescriptorCategory) o));
      } else {
        throw new UnsupportedOperationException(o.getClass().getName());
      }
    }

    if (capability instanceof StatisticsCapability) {
      map.put("properties", toMap(((StatisticsCapability) capability).getProperties()));
    }
    return map;
  }

  private static Map<String, Object> toMap(StatisticsCapability.Properties properties) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("averageWindowDuration", properties.getAverageWindowDuration());
    map.put("averageWindowUnit", properties.getAverageWindowUnit().name());
    map.put("historyInterval", properties.getHistoryInterval());
    map.put("historyIntervalUnit", properties.getHistoryIntervalUnit().name());
    map.put("historySize", properties.getHistorySize());
    map.put("timeToDisable", properties.getTimeToDisable());
    map.put("timeToDisableUnit", properties.getTimeToDisableUnit().name());
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
    map.put("type", descriptor.getType().name());
    return map;
  }

  private static Map<String, Object> toMap(StatisticDescriptorCategory descriptor) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("name", descriptor.getName());
    map.put("statistics", descriptor.getStatistics().stream().map(Manageable::toMap).collect(Collectors.toList()));
    return map;
  }

  private static Map<String, Object> toMap(CallDescriptor descriptor) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("name", descriptor.getName());
    map.put("returnType", descriptor.getReturnType());
    map.put("parameters", descriptor.getParameters().stream().map(Manageable::toMap).collect(Collectors.toList()));
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
    map.put("subContexts", contextContainer.getSubContexts().stream().map(Manageable::toMap).collect(Collectors.toList()));
    return map;
  }

  public static Manageable create(String manageableName, String type) {
    return new Manageable(key(manageableName, type), manageableName, type);
  }

  public static String key(String manageableName, String type) {
    return manageableName + ":" + type;
  }

}
