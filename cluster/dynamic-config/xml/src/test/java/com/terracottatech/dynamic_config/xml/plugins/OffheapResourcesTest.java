/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.xml.plugins;

import org.junit.Test;
import org.terracotta.offheapresource.config.OffheapResourcesType;
import org.terracotta.offheapresource.config.ResourceType;

import com.terracottatech.dynamic_config.config.Measure;
import com.terracottatech.utilities.MemoryUnit;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.*;

public class OffheapResourcesTest {

  @Test
  public void testCreateOffheapResourcesType() {
    Map<String, Measure<MemoryUnit>> expected = new HashMap<>();
    expected.put("first", Measure.of(10, MemoryUnit.GB));
    expected.put("second", Measure.of(20, MemoryUnit.MB));

    OffheapResourcesType offheapResourcesType = new OffheapResources(expected).createOffheapResourcesType();

    assertThat(offheapResourcesType, notNullValue());
    Map<String, Measure<MemoryUnit>> actual = new HashMap<>();
    for (ResourceType resourceType : offheapResourcesType.getResource()) {
      actual.put(resourceType.getName(), Measure.of(resourceType.getValue().intValue(),
                                                    MemoryUnit.valueOf(resourceType.getUnit().value())));
    }

    assertThat(actual, is(expected));
  }
}