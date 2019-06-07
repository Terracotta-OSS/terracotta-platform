/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.nomad;

import com.terracottatech.utilities.Measure;
import com.terracottatech.utilities.MemoryUnit;
import org.terracotta.offheapresource.OffHeapResource;

import java.util.Map;

public class TCConfigController implements ConfigController {

  private final Map<String, OffHeapResource> offheapResources;

  public TCConfigController(Map<String, OffHeapResource> offheapResources) {
    this.offheapResources = offheapResources;
  }

  @Override
  public Measure<MemoryUnit> getOffheapSize(String name) throws ConfigControllerException {
    OffHeapResource offheapResource = getOffheapResource(name);
    return Measure.of(offheapResource.capacity(), MemoryUnit.B);
  }

  @Override
  public void setOffheapSize(String name, Measure<MemoryUnit> newOffheapSize) throws ConfigControllerException {
    OffHeapResource offheapResource = getOffheapResource(name);
    boolean success = offheapResource.setCapacity(newOffheapSize.getQuantity(MemoryUnit.B));
    if (!success) {
      throw new ConfigControllerException("Failed to change size of offheap: " + name + " to new size: " + newOffheapSize + " existing size: " + offheapResource.capacity());
    }
  }

  private OffHeapResource getOffheapResource(String offheapName) throws ConfigControllerException {
    OffHeapResource offheapResource = offheapResources.get(offheapName);

    if (offheapResource == null) {
      throw new ConfigControllerException("Unknown offheap resource: " + offheapName);
    }

    return offheapResource;
  }

  @Override
  public String getStripeName() {
    return "stripe1";
  }

  @Override
  public String getNodeName() {
    return "node0";
  }
}
