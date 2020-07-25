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

import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.Setting;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.dynamic_config.server.api.ConfigChangeHandlerManager;
import org.terracotta.dynamic_config.server.api.DynamicConfigExtension;
import org.terracotta.entity.PlatformConfiguration;

/**
 * @author Mathieu Carbou
 */
public class OffHeapDynamicConfigExtension implements DynamicConfigExtension {
  @Override
  public void configure(Registrar registrar, PlatformConfiguration platformConfiguration) {
    TopologyService topologyService = findService(platformConfiguration, TopologyService.class);
    ConfigChangeHandlerManager configChangeHandlerManager = findService(platformConfiguration, ConfigChangeHandlerManager.class);

    NodeContext nodeContext = topologyService.getRuntimeNodeContext();
    OffHeapResourcesProvider offHeapResourcesProvider = new OffHeapResourcesProvider(nodeContext.getCluster().getOffheapResources().orDefault());
    configChangeHandlerManager.set(Setting.OFFHEAP_RESOURCES, new OffheapResourceConfigChangeHandler(topologyService, offHeapResourcesProvider));

    registrar.registerExtendedConfiguration(offHeapResourcesProvider);
  }
}
