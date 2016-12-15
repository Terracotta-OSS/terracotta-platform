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

import org.terracotta.management.model.call.Parameter;
import org.terracotta.management.model.cluster.Cluster;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.message.Message;
import org.terracotta.management.service.monitoring.ConsumerManagementRegistry;
import org.terracotta.management.service.monitoring.EntityMonitoringService;
import org.terracotta.management.service.monitoring.SharedManagementRegistry;
import org.terracotta.voltron.proxy.ClientId;

import java.util.List;
import java.util.concurrent.Future;

/**
 * @author Mathieu Carbou
 */
class PassiveTmsAgent extends AbstractTmsAgent {

  PassiveTmsAgent(ConsumerManagementRegistry consumerManagementRegistry, EntityMonitoringService entityMonitoringService, SharedManagementRegistry sharedManagementRegistry) {
    super(consumerManagementRegistry, entityMonitoringService, sharedManagementRegistry);
  }

  @Override
  public Future<Cluster> readTopology() {
    throw new UnsupportedOperationException("Cannot be called on a passive server");
  }

  @Override
  public synchronized Future<List<Message>> readMessages() {
    throw new UnsupportedOperationException("Cannot be called on a passive server");
  }

  @Override
  public Future<String> call(@ClientId Object callerDescriptor, Context context, String capabilityName, String methodName, Class<?> returnType, Parameter... parameters) {
    throw new UnsupportedOperationException("Cannot be called on a passive server");
  }

}
