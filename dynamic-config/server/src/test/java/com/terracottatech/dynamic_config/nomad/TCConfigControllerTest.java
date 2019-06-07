/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.nomad;

import com.terracottatech.utilities.Measure;
import com.terracottatech.utilities.MemoryUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.terracotta.offheapresource.OffHeapResource;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TCConfigControllerTest {
  @Mock
  private OffHeapResource offheapResource;

  private TCConfigController controller;

  @Before
  public void before() {
    Map<String, OffHeapResource> offheapResources = new HashMap<>();
    offheapResources.put("name", offheapResource);
    when(offheapResource.capacity()).thenReturn(5L);
    controller = new TCConfigController(offheapResources);
  }

  @Test
  public void getSize() throws Exception {
    assertThat(controller.getOffheapSize("name"), is(Measure.of(5L, MemoryUnit.B)));
  }

  @Test(expected = ConfigControllerException.class)
  public void getSizeUnknownName() throws Exception {
    controller.getOffheapSize("unknown");
  }

  @Test
  public void setSizeSuccess() throws Exception {
    when(offheapResource.setCapacity(10L)).thenReturn(true);
    controller.setOffheapSize("name", Measure.of(10L, MemoryUnit.B));
    verify(offheapResource).setCapacity(10L);
  }

  @Test(expected = ConfigControllerException.class)
  public void setSizeFail() throws Exception {
    when(offheapResource.setCapacity(4L)).thenReturn(false);
    controller.setOffheapSize("name", Measure.of(4L, MemoryUnit.B));
  }
}
