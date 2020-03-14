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
package org.terracotta.offheapresource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.common.struct.Measure;
import org.terracotta.common.struct.MemoryUnit;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Configuration;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.dynamic_config.server.api.ConfigChangeHandler;
import org.terracotta.dynamic_config.server.api.InvalidConfigChangeException;

/**
 * @author Mathieu Carbou
 */
public class OffheapResourceConfigChangeHandler implements ConfigChangeHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(OffheapResourceConfigChangeHandler.class);

  private final TopologyService topologyService;
  private final OffHeapResources offHeapResources;

  public OffheapResourceConfigChangeHandler(TopologyService topologyService, OffHeapResources offHeapResources) {
    this.topologyService = topologyService;
    this.offHeapResources = offHeapResources;
  }

  @Override
  public void validate(NodeContext baseConfig, Configuration change) throws InvalidConfigChangeException {
    if (change.getValue() == null) {
      throw new InvalidConfigChangeException("Invalid change: " + change);
    }

    try {
      Measure<MemoryUnit> measure = Measure.parse(change.getValue(), MemoryUnit.class);
      String name = change.getKey();
      long newValue = measure.getQuantity(MemoryUnit.B);
      OffHeapResource offHeapResource = offHeapResources.getOffHeapResource(OffHeapResourceIdentifier.identifier(name));
      if (offHeapResource != null) {
        if (newValue <= offHeapResource.capacity()) {
          throw new InvalidConfigChangeException("New offheap-resource size: " + change.getValue() +
              " should be larger than the old size: " + Measure.of(offHeapResource.capacity(), measure.getUnit()));
        }
      }

      Cluster updatedCluster = baseConfig.getCluster();
      change.apply(updatedCluster);

      LOGGER.debug("Validating the update cluster: {} against the license", updatedCluster);
      topologyService.validateAgainstLicense(updatedCluster);
    } catch (RuntimeException e) {
      throw new InvalidConfigChangeException(e.getMessage(), e);
    }
  }

  @Override
  public void apply(Configuration change) {
    OffHeapResourceIdentifier identifier = OffHeapResourceIdentifier.identifier(change.getKey());
    OffHeapResource offHeapResource = offHeapResources.getOffHeapResource(identifier);
    Measure<MemoryUnit> measure = Measure.parse(change.getValue(), MemoryUnit.class);

    if (offHeapResource == null) {
      offHeapResources.addOffHeapResource(identifier, measure.getQuantity(MemoryUnit.B));
      LOGGER.debug("Added offheap-resource: {} with capacity: {}", change.getKey(), change.getValue());
    } else {
      offHeapResource.setCapacity(measure.getQuantity(MemoryUnit.B));
      LOGGER.debug("Set the capacity of offheap-resource: {} to: {}", change.getKey(), measure);
    }
  }
}
