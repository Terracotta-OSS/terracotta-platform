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
