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
package org.terracotta.management.entity.management.server;

import org.terracotta.entity.BasicServiceConfiguration;
import org.terracotta.entity.ClientCommunicator;
import org.terracotta.entity.ServiceRegistry;
import org.terracotta.management.entity.management.ManagementAgent;
import org.terracotta.management.entity.management.ManagementAgentConfig;
import org.terracotta.management.entity.management.ManagementAgentVersion;
import org.terracotta.management.model.message.Message;
import org.terracotta.management.service.monitoring.ClientMonitoringService;
import org.terracotta.management.service.monitoring.ClientMonitoringServiceConfiguration;
import org.terracotta.voltron.proxy.SerializationCodec;
import org.terracotta.voltron.proxy.server.ProxyServerEntityService;

import java.util.Objects;

/**
 * @author Mathieu Carbou
 */
public class ManagementAgentEntityServerService extends ProxyServerEntityService<ManagementAgentConfig> {

  public ManagementAgentEntityServerService() {
    //TODO: MATHIEU - PERF: https://github.com/Terracotta-OSS/terracotta-platform/issues/92
    super(ManagementAgent.class, ManagementAgentConfig.class, new SerializationCodec(), Message.class);
  }

  @Override
  public ManagementAgentServerEntity createActiveEntity(ServiceRegistry registry, ManagementAgentConfig configuration) {
    ClientCommunicator communicator = Objects.requireNonNull(registry.getService(new BasicServiceConfiguration<>(ClientCommunicator.class)));
    ClientMonitoringService clientMonitoringService = Objects.requireNonNull(registry.getService(new ClientMonitoringServiceConfiguration(communicator)));
    ManagementAgentImpl managementAgent = new ManagementAgentImpl(clientMonitoringService);
    return new ManagementAgentServerEntity(managementAgent, communicator);
  }

  @Override
  public long getVersion() {
    return ManagementAgentVersion.LATEST.version();
  }

  @Override
  public boolean handlesEntityType(String typeName) {
    return ManagementAgentConfig.ENTITY_TYPE.equals(typeName);
  }
}
