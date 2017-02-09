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
package org.terracotta.management.entity.nms.agent.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.entity.BasicServiceConfiguration;
import org.terracotta.entity.ClientCommunicator;
import org.terracotta.entity.ServiceRegistry;
import org.terracotta.management.entity.nms.agent.NmsAgent;
import org.terracotta.management.entity.nms.agent.NmsAgentConfig;
import org.terracotta.management.entity.nms.agent.NmsAgentVersion;
import org.terracotta.management.entity.nms.agent.ReconnectData;
import org.terracotta.management.model.message.Message;
import org.terracotta.management.service.monitoring.ClientMonitoringService;
import org.terracotta.management.service.monitoring.ClientMonitoringServiceConfiguration;
import org.terracotta.voltron.proxy.SerializationCodec;
import org.terracotta.voltron.proxy.server.ProxyServerEntityService;

import java.util.Objects;

/**
 * @author Mathieu Carbou
 */
public class NmsAgentEntityServerService extends ProxyServerEntityService<NmsAgent, NmsAgentConfig, Void, ReconnectData, Void> {

  private static final Logger LOGGER = LoggerFactory.getLogger(NmsAgentEntityServerService.class);

  public NmsAgentEntityServerService() {
    //TODO: MATHIEU - PERF: https://github.com/Terracotta-OSS/terracotta-platform/issues/92
    super(NmsAgent.class, NmsAgentConfig.class, new Class<?>[]{Message.class}, null, ReconnectData.class, null);
    setCodec(new SerializationCodec());
  }

  @Override
  public ActiveNmsAgentServerEntity createActiveEntity(ServiceRegistry registry, NmsAgentConfig configuration) {
    ClientCommunicator communicator = Objects.requireNonNull(registry.getService(new BasicServiceConfiguration<>(ClientCommunicator.class)));
    ClientMonitoringService clientMonitoringService = Objects.requireNonNull(registry.getService(new ClientMonitoringServiceConfiguration(communicator)));
    ActiveNmsAgent activeNmsAgent = new ActiveNmsAgent(clientMonitoringService);
    LOGGER.trace("createActiveEntity()");
    return new ActiveNmsAgentServerEntity(activeNmsAgent);
  }

  @Override
  protected PassiveNmsAgentServerEntity createPassiveEntity(ServiceRegistry registry, NmsAgentConfig configuration) {
    LOGGER.trace("createPassiveEntity()");
    return new PassiveNmsAgentServerEntity(new PassiveNmsAgent());
  }

  @Override
  public long getVersion() {
    return NmsAgentVersion.LATEST.version();
  }

  @Override
  public boolean handlesEntityType(String typeName) {
    return NmsAgentConfig.ENTITY_TYPE.equals(typeName);
  }
}
