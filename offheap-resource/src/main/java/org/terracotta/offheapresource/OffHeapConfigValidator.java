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

import org.terracotta.config.service.ConfigValidator;
import org.terracotta.config.service.ValidationException;
import org.terracotta.offheapresource.config.OffheapResourcesType;
import org.terracotta.offheapresource.config.ResourceType;
import org.w3c.dom.Element;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.terracotta.offheapresource.OffHeapResourcesProvider.convert;
import static org.terracotta.offheapresource.OffHeapResourcesProvider.longValueExact;

public class OffHeapConfigValidator implements ConfigValidator {
  
  private final Function<Element, OffheapResourcesType> resourcesTypeSupplier;

  public OffHeapConfigValidator(Function<Element, OffheapResourcesType> resourcesTypeSupplier) {
    this.resourcesTypeSupplier = resourcesTypeSupplier;
  }

  public void validateAgainst(Element oneFragment, Element otherFragment) throws ValidationException {
    OffheapResourcesType oneOffHeapObj = createObject(oneFragment);
    OffheapResourcesType otherOffHeapObj = createObject(otherFragment);

    if (oneOffHeapObj.getResource().size() != otherOffHeapObj.getResource().size()) {
      throw new ValidationException("Number of Off Heap definitions are not matching."
          , ValidationFailureId.DIFFERENT_NUMBER_OF_OFFHEAP_DEFINITIONS.getFailureId());
    }
    //This call covers previous case as well; explicitly compared the sizes so that correct error message can be reported in the exception

    Map<String, ResourceType> oneMap = createOffHeapResourceMap(oneOffHeapObj);
    Map<String, ResourceType> ptherMap = createOffHeapResourceMap(otherOffHeapObj);

    if (!compareOffHeapResources(oneMap, ptherMap)) {
      throw new ValidationException("Off Heap entries are not matching"
          , ValidationFailureId.MISMATCHED_OFFHEAPS.getFailureId());
    }

  }

  protected Map<String, ResourceType> createOffHeapResourceMap(OffheapResourcesType offheapResourcesType) {
    return offheapResourcesType.getResource().stream().collect(Collectors.toMap(
        ResourceType::getName, Function.identity()));
  }

  protected boolean compareOffHeapResources(Map<String, ResourceType> oneMap, Map<String, ResourceType> otherMap) {
    Set<String> oneNameSet = oneMap.keySet();
    Set<String> otherNameSet = otherMap.keySet();
    if (!oneNameSet.equals(otherNameSet)) {
      return false;
    }
    for (ResourceType one : oneMap.values()) {
      if (!(compareOffHeapResourceSizes(one, otherMap.get(one.getName())))) {
        return false;
      }
    }
    return true;
  }

  protected boolean compareOffHeapResourceSizes(ResourceType oneResourceType, ResourceType otherResourceType) {
    return longValueExact(convert(oneResourceType.getValue(), oneResourceType.getUnit())) 
           == longValueExact(convert(otherResourceType.getValue(), otherResourceType.getUnit()));
  }

  protected OffheapResourcesType createObject(Element element) {
    return this.resourcesTypeSupplier.apply(element);
  }
}
