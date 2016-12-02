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
package org.terracotta.management.entity.tms.server;

import org.terracotta.entity.BasicServiceConfiguration;
import org.terracotta.entity.ClientCommunicator;
import org.terracotta.entity.ServiceRegistry;
import org.terracotta.management.entity.tms.TmsAgent;
import org.terracotta.management.entity.tms.TmsAgentConfig;
import org.terracotta.management.entity.tms.TmsAgentVersion;
import org.terracotta.management.model.message.Message;
import org.terracotta.management.service.monitoring.ActiveEntityMonitoringService;
import org.terracotta.management.service.monitoring.ActiveEntityMonitoringServiceConfiguration;
import org.terracotta.management.service.monitoring.ConsumerManagementRegistry;
import org.terracotta.management.service.monitoring.ConsumerManagementRegistryConfiguration;
import org.terracotta.management.service.monitoring.ManagementService;
import org.terracotta.management.service.monitoring.ManagementServiceConfiguration;
import org.terracotta.voltron.proxy.SerializationCodec;
import org.terracotta.voltron.proxy.server.ProxyServerEntityService;

import java.util.Objects;

/**
 * @author Mathieu Carbou
 */
public class TmsAgentEntityServerService extends ProxyServerEntityService<TmsAgentConfig> {

  public TmsAgentEntityServerService() {
    super(TmsAgent.class, TmsAgentConfig.class, new SerializationCodec(), Message.class);
  }

  @Override
  public TmsAgentServerEntity createActiveEntity(ServiceRegistry registry, TmsAgentConfig tmsAgentConfig) {
    ClientCommunicator communicator = Objects.requireNonNull(registry.getService(new BasicServiceConfiguration<>(ClientCommunicator.class)));
    ManagementService managementService = Objects.requireNonNull(registry.getService(new ManagementServiceConfiguration(communicator)));
    ActiveEntityMonitoringService activeEntityMonitoringService = Objects.requireNonNull(registry.getService(new ActiveEntityMonitoringServiceConfiguration()));
    ConsumerManagementRegistry consumerManagementRegistry = Objects.requireNonNull(registry.getService(new ConsumerManagementRegistryConfiguration(activeEntityMonitoringService)
        .addServerManagementProviders()
        .setStatisticConfiguration(tmsAgentConfig.getStatisticConfiguration())));
    return new TmsAgentServerEntity(new TmsAgentImpl(tmsAgentConfig, managementService, consumerManagementRegistry), communicator);
  }

  @Override
  public long getVersion() {
    return TmsAgentVersion.LATEST.version();
  }

  @Override
  public boolean handlesEntityType(String typeName) {
    return TmsAgentConfig.ENTITY_TYPE.equals(typeName);
  }


}
