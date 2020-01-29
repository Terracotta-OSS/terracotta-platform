/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.xml.plugins;

import org.terracotta.common.struct.Measure;
import org.terracotta.common.struct.MemoryUnit;
import org.terracotta.dynamic_config.xml.Utils;
import org.terracotta.offheapresource.config.ObjectFactory;
import org.terracotta.offheapresource.config.OffheapResourcesType;
import org.terracotta.offheapresource.config.ResourceType;
import org.w3c.dom.Element;

import javax.xml.bind.JAXBElement;
import java.math.BigInteger;
import java.util.Map;

public class OffheapResources {
  private static final ObjectFactory FACTORY = new ObjectFactory();

  private final Map<String, Measure<MemoryUnit>> offheapResourcesMap;

  public OffheapResources(Map<String, Measure<MemoryUnit>> offheapResourcesMap) {
    this.offheapResourcesMap = offheapResourcesMap;
  }

  public Element toElement() {
    OffheapResourcesType offheapResourcesType = createOffheapResourcesType();

    JAXBElement<OffheapResourcesType> offheapResources = FACTORY.createOffheapResources(offheapResourcesType);

    return Utils.createElement(offheapResources);
  }

  OffheapResourcesType createOffheapResourcesType() {
    OffheapResourcesType offheapResourcesType = FACTORY.createOffheapResourcesType();

    for (Map.Entry<String, Measure<MemoryUnit>> entry : offheapResourcesMap.entrySet()) {
      ResourceType resourceType = FACTORY.createResourceType();

      resourceType.setName(entry.getKey());

      resourceType.setValue(BigInteger.valueOf(entry.getValue().getQuantity()));
      resourceType.setUnit(org.terracotta.offheapresource.config.MemoryUnit.valueOf(entry.getValue().getUnit().name()));

      offheapResourcesType.getResource().add(resourceType);
    }

    return offheapResourcesType;
  }
}
