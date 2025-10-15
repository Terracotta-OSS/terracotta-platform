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
package org.terracotta.stats.entity.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.entity.BasicServiceConfiguration;
import org.terracotta.entity.ConfigurationException;
import org.terracotta.entity.ServiceException;
import org.terracotta.entity.ServiceRegistry;
import org.terracotta.management.registry.CombiningCapabilityManagementSupport;
import org.terracotta.management.service.monitoring.EntityManagementRegistry;
import org.terracotta.management.service.monitoring.ServerManagementRegistryConfiguration;
import org.terracotta.management.service.monitoring.SharedEntityManagementRegistry;
import org.terracotta.stats.entity.common.Stats;
import org.terracotta.stats.entity.common.StatsConfig;
import org.terracotta.stats.entity.common.StatsVersion;
import org.terracotta.voltron.proxy.SerializationCodec;
import org.terracotta.voltron.proxy.server.ProxyServerEntityService;

import java.util.Objects;

import static java.util.regex.Pattern.compile;

public class StatsEntityServerService extends ProxyServerEntityService<StatsConfig, Void, Void, StatsCallback> {

  private static final Logger LOGGER = LoggerFactory.getLogger(StatsEntityServerService.class);

  public StatsEntityServerService() {
    super(Stats.class, StatsConfig.class, new Class<?>[]{}, null, null, StatsCallback.class);
    setCodec(new SerializationCodec(compile("^(?:org\\.ehcache\\.shadow|com\\.terracottatech\\.shadow)\\.(org\\.terracotta\\.statistics\\..*)$")));
  }

  @Override
  public ActiveStatsServerEntity createActiveEntity(ServiceRegistry registry, StatsConfig configuration) throws ConfigurationException {
    LOGGER.trace("createActiveEntity()");
    try {
      TopologyService topologyService = Objects.requireNonNull(registry.getService(new BasicServiceConfiguration<>(TopologyService.class)));
      EntityManagementRegistry entityManagementRegistry = Objects.requireNonNull(registry.getService(new ServerManagementRegistryConfiguration(registry, true)));
      SharedEntityManagementRegistry sharedEntityManagementRegistry = Objects.requireNonNull(registry.getService(new BasicServiceConfiguration<>(SharedEntityManagementRegistry.class)));
      ActiveStatsServerEntity entity = new ActiveStatsServerEntity(configuration, entityManagementRegistry, sharedEntityManagementRegistry, topologyService);
      return entity;
    } catch (ServiceException e) {
      throw new ConfigurationException("Unable to retrieve service: " + e.getMessage());
    }
  }

  @Override
  protected PassiveStatsServerEntity createPassiveEntity(ServiceRegistry registry, StatsConfig configuration) throws ConfigurationException {
    LOGGER.trace("createPassiveEntity()");
    return new PassiveStatsServerEntity();
  }

  @Override
  public long getVersion() {
    return StatsVersion.LATEST.version();
  }

  @Override
  public boolean handlesEntityType(String typeName) {
    return StatsConfig.ENTITY_TYPE.equals(typeName);
  }

}
