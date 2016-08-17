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
package org.terracotta.management.tms.entity.server;

import org.terracotta.management.tms.entity.TmsAgent;
import org.terracotta.management.tms.entity.TmsAgentConfig;
import org.terracotta.management.tms.entity.Version;
import org.terracotta.entity.BasicServiceConfiguration;
import org.terracotta.entity.ServiceRegistry;
import org.terracotta.management.sequence.BoundaryFlakeSequenceGenerator;
import org.terracotta.management.sequence.SequenceGenerator;
import org.terracotta.management.service.monitoring.IMonitoringConsumer;
import org.terracotta.management.service.monitoring.MonitoringConsumerConfiguration;
import org.terracotta.voltron.proxy.SerializationCodec;
import org.terracotta.voltron.proxy.server.ProxyServerEntityService;

/**
 * @author Mathieu Carbou
 */
public class TmsAgentEntityServerService extends ProxyServerEntityService<TmsAgentConfig> {

  public TmsAgentEntityServerService() {
    super(TmsAgent.class, TmsAgentConfig.class, new SerializationCodec());
  }

  @Override
  public TmsAgentServerEntity createActiveEntity(ServiceRegistry registry, TmsAgentConfig tmsAgentConfig) {
    IMonitoringConsumer consumer = registry.getService(new MonitoringConsumerConfiguration().recordMutations());
    SequenceGenerator sequenceGenerator = registry.getService(new BasicServiceConfiguration<>(SequenceGenerator.class));
    if (sequenceGenerator == null) {
      sequenceGenerator = new BoundaryFlakeSequenceGenerator();
    }
    return new TmsAgentServerEntity(tmsAgentConfig, consumer, sequenceGenerator);
  }

  @Override
  public long getVersion() {
    return Version.LATEST.version();
  }

  @Override
  public boolean handlesEntityType(String typeName) {
    return TmsAgentConfig.ENTITY_TYPE.equals(typeName);
  }


}
