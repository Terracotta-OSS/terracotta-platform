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

import org.terracotta.management.entity.nms.agent.NmsAgent;
import org.terracotta.management.model.call.ContextualReturn;
import org.terracotta.management.model.capabilities.Capability;
import org.terracotta.management.model.context.ContextContainer;
import org.terracotta.management.model.notification.ContextualNotification;
import org.terracotta.management.model.stats.ContextualStatistics;
import org.terracotta.voltron.proxy.ClientId;
import org.terracotta.voltron.proxy.server.PassiveProxiedServerEntity;

import java.util.concurrent.Future;

/**
 * @author Mathieu Carbou
 */
class PassiveNmsAgentServerEntity extends PassiveProxiedServerEntity implements NmsAgent {
  @Override
  public Future<Void> exposeManagementMetadata(@ClientId Object clientDescriptor, ContextContainer contextContainer, Capability... capabilities) {
    throw new UnsupportedOperationException("Cannot be called on a passive server");
  }

  @Override
  public Future<Void> exposeTags(@ClientId Object clientDescriptor, String... tags) {
    throw new UnsupportedOperationException("Cannot be called on a passive server");
  }

  @Override
  public Future<Void> answerManagementCall(@ClientId Object clientDescriptor, String managementCallId, ContextualReturn<?> contextualReturn) {
    throw new UnsupportedOperationException("Cannot be called on a passive server");
  }

  @Override
  public Future<Void> pushNotification(@ClientId Object clientDescriptor, ContextualNotification notification) {
    throw new UnsupportedOperationException("Cannot be called on a passive server");
  }

  @Override
  public Future<Void> pushStatistics(@ClientId Object clientDescriptor, ContextualStatistics... statistics) {
    throw new UnsupportedOperationException("Cannot be called on a passive server");
  }
}
