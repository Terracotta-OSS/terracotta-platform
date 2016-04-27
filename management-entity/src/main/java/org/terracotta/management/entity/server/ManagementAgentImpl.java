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

import org.terracotta.entity.ClientDescriptor;
import org.terracotta.management.entity.ManagementAgent;
import org.terracotta.management.entity.ManagementAgentConfig;
import org.terracotta.management.model.capabilities.Capability;
import org.terracotta.management.model.context.ContextContainer;
import org.terracotta.monitoring.IMonitoringProducer;
import org.terracotta.voltron.proxy.ClientId;

import java.util.Collection;
import java.util.concurrent.Future;

import static org.terracotta.management.entity.server.Utils.array;
import static org.terracotta.management.entity.server.Utils.completedFuture;

/**
 * @author Mathieu Carbou
 */
public class ManagementAgentImpl implements ManagementAgent {

  private final ManagementAgentConfig config;
  private final IMonitoringProducer producer;

  public ManagementAgentImpl(ManagementAgentConfig config, IMonitoringProducer producer) {
    this.config = config;
    this.producer = producer;
  }

  @Override
  public Future<Void> expose(ContextContainer contextContainer, Collection<Capability> capabilities, @ClientId Object clientId) {
    ClientDescriptor clientDescriptor = (ClientDescriptor) clientId;
    //TODO: MATHIEU - expose management metadata on the right connection found thanks to the ClientDescriptor

    return completedFuture(null);
  }

}
