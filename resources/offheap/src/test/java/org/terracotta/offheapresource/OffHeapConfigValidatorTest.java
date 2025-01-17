/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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
package org.terracotta.offheapresource;

import org.junit.Test;
import org.terracotta.config.service.ValidationException;
import org.terracotta.offheapresource.config.MemoryUnit;
import org.terracotta.offheapresource.config.OffheapResourcesType;
import org.terracotta.offheapresource.config.ResourceType;
import org.w3c.dom.Element;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
public class OffHeapConfigValidatorTest {
  @Test
  public void testValidateWithMismatchedOffHeapResourceSize() {
    Function<Element, OffheapResourcesType> OffheapResourcesType = mock(Function.class);
    OffHeapConfigValidator validator = new OffHeapConfigValidator(OffheapResourcesType);
    OffHeapConfigValidator spyValidator = spy(validator);

    Element offHeapNode1 = mock(Element.class);
    Element offHeapNode2 = mock(Element.class);


    OffheapResourcesType offHeapResource1 = mock(OffheapResourcesType.class);
    OffheapResourcesType offHeapResource2 = mock(OffheapResourcesType.class);

    @SuppressWarnings("unchecked")
    List<ResourceType> resourceList1 = mock(List.class);
    @SuppressWarnings("unchecked")
    List<ResourceType> resourceList2 = mock(List.class);

    when(offHeapResource1.getResource()).thenReturn(resourceList1);
    when(offHeapResource2.getResource()).thenReturn(resourceList2);


    doReturn(offHeapResource1).when(spyValidator).createObject(offHeapNode1);
    doReturn(offHeapResource2).when(spyValidator).createObject(offHeapNode2);

    when(resourceList1.size()).thenReturn(2);
    when(resourceList2.size()).thenReturn(3);

    try {
      spyValidator.validateAgainst(offHeapNode1, offHeapNode2);
      fail("Expected ValidationException");
    } catch (ValidationException e) {
      assertThat(e.getErrorId(), is(ValidationFailureId.DIFFERENT_NUMBER_OF_OFFHEAP_DEFINITIONS.getFailureId()));
    }
  }

  @Test
  public void testValidateWithMismatchedOffHeapResourceNames() {
    Function<Element, OffheapResourcesType> OffheapResourcesType = mock(Function.class);
    OffHeapConfigValidator validator = new OffHeapConfigValidator(OffheapResourcesType);
    OffHeapConfigValidator spyValidator = spy(validator);

    Element offHeapNode1 = mock(Element.class);
    Element offHeapNode2 = mock(Element.class);

    ResourceType node11 = new ResourceType();
    node11.setName("offHeap1");
    node11.setValue(BigInteger.valueOf(1024L));
    node11.setUnit(MemoryUnit.MB);

    ResourceType node12 = new ResourceType();
    node12.setName("offHeap2");
    node12.setValue(BigInteger.valueOf(1024L));
    node12.setUnit(MemoryUnit.MB);

    ResourceType node21 = new ResourceType();
    node21.setName("offHeap1");
    node21.setValue(BigInteger.valueOf(1024L));
    node21.setUnit(MemoryUnit.MB);

    ResourceType node22 = new ResourceType();
    node22.setName("offHeap-x");
    node22.setValue(BigInteger.valueOf(512L));
    node22.setUnit(MemoryUnit.MB);

    OffheapResourcesType offHeapResource1 = new OffheapResourcesType();
    offHeapResource1.getResource().add(node11);
    offHeapResource1.getResource().add(node12);

    OffheapResourcesType offHeapResource2 = new OffheapResourcesType();
    offHeapResource2.getResource().add(node21);
    offHeapResource2.getResource().add(node22);

    doReturn(offHeapResource1).when(spyValidator).createObject(offHeapNode1);
    doReturn(offHeapResource2).when(spyValidator).createObject(offHeapNode2);

    try {
      spyValidator.validateAgainst(offHeapNode1, offHeapNode2);
      fail("Expected ValidationException");
    } catch (ValidationException e) {
      assertThat(e.getErrorId(), is(ValidationFailureId.MISMATCHED_OFFHEAPS.getFailureId()));
    }

  }

  @Test
  public void testValidateWithMismatchedOffHeapResourceValues() {
    Function<Element, OffheapResourcesType> OffheapResourcesType = mock(Function.class);
    OffHeapConfigValidator validator = new OffHeapConfigValidator(OffheapResourcesType);
    OffHeapConfigValidator spyValidator = spy(validator);

    Element offHeapNode1 = mock(Element.class);
    Element offHeapNode2 = mock(Element.class);

    ResourceType node11 = new ResourceType();
    node11.setName("offHeap1");
    node11.setValue(BigInteger.valueOf(1024L));
    node11.setUnit(MemoryUnit.MB);

    ResourceType node12 = new ResourceType();
    node12.setName("offHeap2");
    node12.setValue(BigInteger.valueOf(512L));
    node12.setUnit(MemoryUnit.MB);

    ResourceType node21 = new ResourceType();
    node21.setName("offHeap1");
    node21.setValue(BigInteger.valueOf(1024L));
    node21.setUnit(MemoryUnit.MB);

    ResourceType node22 = new ResourceType();
    node22.setName("offHeap2");
    node22.setValue(BigInteger.valueOf(1024L));
    node22.setUnit(MemoryUnit.MB);

    OffheapResourcesType offHeapResource1 = new OffheapResourcesType();
    offHeapResource1.getResource().add(node11);
    offHeapResource1.getResource().add(node12);

    OffheapResourcesType offHeapResource2 = new OffheapResourcesType();
    offHeapResource2.getResource().add(node21);
    offHeapResource2.getResource().add(node22);

    doReturn(offHeapResource1).when(spyValidator).createObject(offHeapNode1);
    doReturn(offHeapResource2).when(spyValidator).createObject(offHeapNode2);

    try {
      spyValidator.validateAgainst(offHeapNode1, offHeapNode2);
      fail("Expected ValidationException");
    } catch (ValidationException e) {
      assertThat(e.getErrorId(), is(ValidationFailureId.MISMATCHED_OFFHEAPS.getFailureId()));
    }
  }

  @Test
  public void testValidateWithMismatchedOffHeapResourceValueUnits() {
    Function<Element, OffheapResourcesType> OffheapResourcesType = mock(Function.class);
    OffHeapConfigValidator validator = new OffHeapConfigValidator(OffheapResourcesType);
    OffHeapConfigValidator spyValidator = spy(validator);

    Element offHeapNode1 = mock(Element.class);
    Element offHeapNode2 = mock(Element.class);

    ResourceType node11 = new ResourceType();
    node11.setName("offHeap1");
    node11.setValue(BigInteger.valueOf(1024L));
    node11.setUnit(MemoryUnit.MB);

    ResourceType node12 = new ResourceType();
    node12.setName("offHeap2");
    node12.setValue(BigInteger.valueOf(512L));
    node12.setUnit(MemoryUnit.MB);

    ResourceType node21 = new ResourceType();
    node21.setName("offHeap1");
    node21.setValue(BigInteger.valueOf(1024L));
    node21.setUnit(MemoryUnit.MB);

    ResourceType node22 = new ResourceType();
    node22.setName("offHeap2");
    node22.setValue(BigInteger.valueOf(1024L));
    node22.setUnit(MemoryUnit.GB);

    OffheapResourcesType offHeapResource1 = new OffheapResourcesType();
    offHeapResource1.getResource().add(node11);
    offHeapResource1.getResource().add(node12);

    OffheapResourcesType offHeapResource2 = new OffheapResourcesType();
    offHeapResource2.getResource().add(node21);
    offHeapResource2.getResource().add(node22);


    doReturn(offHeapResource1).when(spyValidator).createObject(offHeapNode1);
    doReturn(offHeapResource2).when(spyValidator).createObject(offHeapNode2);

    try {
      spyValidator.validateAgainst(offHeapNode1, offHeapNode2);
      fail("Expected ValidationException");
    } catch (ValidationException e) {
      assertThat(e.getErrorId(), is(ValidationFailureId.MISMATCHED_OFFHEAPS.getFailureId()));
    }
  }

  @Test
  public void testCreateOffHeapResourceMap() {
    Function<Element, OffheapResourcesType> OffheapResourcesType = mock(Function.class);
    OffHeapConfigValidator validator = new OffHeapConfigValidator(OffheapResourcesType);
    OffheapResourcesType offHeapResources = new OffheapResourcesType();

    ResourceType resourceType1 = new ResourceType();
    resourceType1.setUnit(MemoryUnit.MB);
    resourceType1.setValue(BigInteger.valueOf(64L));
    resourceType1.setName("resource1");

    ResourceType resourceType2 = new ResourceType();
    resourceType2.setUnit(MemoryUnit.MB);
    resourceType2.setValue(BigInteger.valueOf(128L));
    resourceType2.setName("resource2");

    ResourceType resourceType3 = new ResourceType();
    resourceType3.setUnit(MemoryUnit.GB);
    resourceType3.setValue(BigInteger.valueOf(64L));
    resourceType3.setName("resource3");

    offHeapResources.getResource().add(resourceType1);
    offHeapResources.getResource().add(resourceType2);
    offHeapResources.getResource().add(resourceType3);

    Map<String, ResourceType> resourceMap = validator.createOffHeapResourceMap(offHeapResources);
    assertThat(resourceMap, notNullValue());
    assertThat(resourceMap.size(), is(3));

    Set<String> expectedResourceNames = new HashSet<>();
    expectedResourceNames.add("resource1");
    expectedResourceNames.add("resource2");
    expectedResourceNames.add("resource3");

    assertThat(resourceMap.keySet().equals(expectedResourceNames), is(true));
    assertThat(resourceMap.get("resource1") == resourceType1, is(true));
    assertThat(resourceMap.get("resource2") == resourceType2, is(true));
    assertThat(resourceMap.get("resource3") == resourceType3, is(true));
  }

  @Test
  public void testCompareOffHeapResources() {
    Function<Element, OffheapResourcesType> OffheapResourcesType = mock(Function.class);
    OffHeapConfigValidator validator = new OffHeapConfigValidator(OffheapResourcesType);

    ResourceType resourceType1 = new ResourceType();
    resourceType1.setUnit(MemoryUnit.MB);
    resourceType1.setValue(BigInteger.valueOf(64L));
    resourceType1.setName("resource1");

    ResourceType resourceType2 = new ResourceType();
    resourceType2.setUnit(MemoryUnit.MB);
    resourceType2.setValue(BigInteger.valueOf(128L));
    resourceType2.setName("resource2");

    ResourceType resourceType3 = new ResourceType();
    resourceType3.setUnit(MemoryUnit.GB);
    resourceType3.setValue(BigInteger.valueOf(64L));
    resourceType3.setName("resource3");

    Map<String, ResourceType> input1 = new HashMap<>();
    input1.put(resourceType1.getName(), resourceType1);
    input1.put(resourceType2.getName(), resourceType2);
    input1.put(resourceType3.getName(), resourceType3);

    ResourceType resourceType4 = new ResourceType();
    resourceType4.setUnit(MemoryUnit.MB);
    resourceType4.setValue(BigInteger.valueOf(64L));
    resourceType4.setName("resource1");

    ResourceType resourceType5 = new ResourceType();
    resourceType5.setUnit(MemoryUnit.MB);
    resourceType5.setValue(BigInteger.valueOf(128L));
    resourceType5.setName("resource2");

    ResourceType resourceType6 = new ResourceType();
    resourceType6.setUnit(MemoryUnit.GB);
    resourceType6.setValue(BigInteger.valueOf(64L));
    resourceType6.setName("resource3");

    Map<String, ResourceType> input2 = new HashMap<>();
    input2.put(resourceType4.getName(), resourceType4);
    input2.put(resourceType5.getName(), resourceType5);
    input2.put(resourceType6.getName(), resourceType6);

    boolean retValue = validator.compareOffHeapResources(input1, input2);
    assertThat(retValue, is(true));

    resourceType6 = new ResourceType();
    resourceType6.setUnit(MemoryUnit.GB);
    resourceType6.setValue(BigInteger.valueOf(64L));
    resourceType6.setName("resource33");

    input2.put(resourceType6.getName(), resourceType6);

    retValue = validator.compareOffHeapResources(input1, input2);

    assertThat(retValue, is(false));

  }

  @Test
  public void testValidate() throws ValidationException {
    Function<Element, OffheapResourcesType> OffheapResourcesType = mock(Function.class);
    OffHeapConfigValidator validator = new OffHeapConfigValidator(OffheapResourcesType);
    OffHeapConfigValidator spyValidator = spy(validator);

    Element offHeapNode1 = mock(Element.class);
    Element offHeapNode2 = mock(Element.class);

    ResourceType node11 = new ResourceType();
    node11.setName("offHeap1");
    node11.setValue(BigInteger.valueOf(1024L));
    node11.setUnit(MemoryUnit.MB);

    ResourceType node12 = new ResourceType();
    node12.setName("offHeap2");
    node12.setValue(BigInteger.valueOf(512L));
    node12.setUnit(MemoryUnit.MB);

    ResourceType node21 = new ResourceType();
    node21.setName("offHeap1");
    node21.setValue(BigInteger.valueOf(1024L));
    node21.setUnit(MemoryUnit.MB);

    ResourceType node22 = new ResourceType();
    node22.setName("offHeap2");
    node22.setValue(BigInteger.valueOf(512L));
    node22.setUnit(MemoryUnit.MB);

    OffheapResourcesType offHeapResource1 = new OffheapResourcesType();
    offHeapResource1.getResource().add(node11);
    offHeapResource1.getResource().add(node12);

    OffheapResourcesType offHeapResource2 = new OffheapResourcesType();
    offHeapResource2.getResource().add(node21);
    offHeapResource2.getResource().add(node22);

    doReturn(offHeapResource1).when(spyValidator).createObject(offHeapNode1);
    doReturn(offHeapResource2).when(spyValidator).createObject(offHeapNode2);

    spyValidator.validateAgainst(offHeapNode1, offHeapNode2);
  }

  @Test
  public void testCompareOffHeapResourceSizes() {
    ResourceType node11 = new ResourceType();
    node11.setName("offHeap1");
    node11.setValue(BigInteger.valueOf(1024L));
    node11.setUnit(MemoryUnit.MB);

    ResourceType node12 = new ResourceType();
    node12.setName("offHeap2");
    node12.setValue(BigInteger.valueOf(512L));
    node12.setUnit(MemoryUnit.MB);

    Function<Element, OffheapResourcesType> OffheapResourcesType = mock(Function.class);
    OffHeapConfigValidator validator = new OffHeapConfigValidator(OffheapResourcesType);
    boolean retValue = validator.compareOffHeapResourceSizes(node11, node12);
    assertThat(retValue, is(false));

    node12 = new ResourceType();
    node12.setName("offHeap2");
    node12.setValue(BigInteger.valueOf(1024L));

    for (MemoryUnit memoryUnit : MemoryUnit.values()) {
      node12.setUnit(memoryUnit);
      retValue = validator.compareOffHeapResourceSizes(node11, node12);
      if (memoryUnit == MemoryUnit.MB) {
        assertThat(retValue, is(true));
      } else {
        assertThat(retValue, is(false));
      }
    }
  }
}
