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
package org.terracotta.management.entity.server;

import org.terracotta.entity.BasicServiceConfiguration;
import org.terracotta.entity.ServiceRegistry;
import org.terracotta.management.entity.ManagementAgent;
import org.terracotta.management.entity.ManagementAgentConfig;
import org.terracotta.management.entity.Version;
import org.terracotta.management.service.monitoring.IMonitoringConsumer;
import org.terracotta.management.service.monitoring.MonitoringConsumerConfiguration;
import org.terracotta.monitoring.IMonitoringProducer;
import org.terracotta.voltron.proxy.SerializationCodec;
import org.terracotta.voltron.proxy.server.ProxiedServerEntity;
import org.terracotta.voltron.proxy.server.ProxyServerEntityService;

/**
 * @author Mathieu Carbou
 */
public class ManagementAgentEntityServerService extends ProxyServerEntityService<ManagementAgentConfig> {

  public ManagementAgentEntityServerService() {
    super(ManagementAgent.class, ManagementAgentConfig.class, new SerializationCodec());
  }

  @Override
  public ProxiedServerEntity<ManagementAgent> createActiveEntity(ServiceRegistry registry, ManagementAgentConfig configuration) {
    IMonitoringProducer producer = registry.getService(new BasicServiceConfiguration<>(IMonitoringProducer.class));
    IMonitoringConsumer consumer = registry.getService(new MonitoringConsumerConfiguration());
    return new ManagementAgentServerEntity(configuration, consumer, producer);
  }

  @Override
  public long getVersion() {
    return Version.LATEST.version();
  }

  @Override
  public boolean handlesEntityType(String typeName) {
    return ManagementAgentConfig.ENTITY_NAME.equals(typeName);
  }
}
