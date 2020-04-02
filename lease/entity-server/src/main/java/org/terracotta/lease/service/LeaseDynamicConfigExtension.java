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
package org.terracotta.lease.service;

import org.terracotta.common.struct.TimeUnit;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.Setting;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.dynamic_config.server.api.ConfigChangeHandlerManager;
import org.terracotta.dynamic_config.server.api.DynamicConfigExtension;
import org.terracotta.entity.PlatformConfiguration;
import org.terracotta.lease.service.config.LeaseConfiguration;

/**
 * @author Mathieu Carbou
 */
public class LeaseDynamicConfigExtension implements DynamicConfigExtension {
  @Override
  public void configure(Registrar registrar, PlatformConfiguration platformConfiguration) {
    TopologyService topologyService = platformConfiguration.getExtendedConfiguration(TopologyService.class).iterator().next();
    ConfigChangeHandlerManager configChangeHandlerManager = platformConfiguration.getExtendedConfiguration(ConfigChangeHandlerManager.class).iterator().next();

    NodeContext nodeContext = topologyService.getRuntimeNodeContext();

    LeaseConfiguration leaseConfiguration = new LeaseConfiguration(nodeContext.getNode().getClientLeaseDuration().getQuantity(TimeUnit.MILLISECONDS));

    configChangeHandlerManager.set(Setting.CLIENT_LEASE_DURATION, new LeaseConfigChangeHandler(leaseConfiguration));

    registrar.registerServiceProviderConfiguration(leaseConfiguration);
  }
}
