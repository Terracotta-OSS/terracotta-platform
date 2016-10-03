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
package org.terracotta.management.service.monitoring;

import com.tc.classloader.CommonComponent;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.management.model.call.ContextualReturn;
import org.terracotta.management.model.call.Parameter;
import org.terracotta.management.model.capabilities.Capability;
import org.terracotta.management.model.cluster.ClientIdentifier;
import org.terracotta.management.model.cluster.Cluster;
import org.terracotta.management.model.cluster.ServerEntityIdentifier;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.context.ContextContainer;
import org.terracotta.management.model.message.Message;
import org.terracotta.management.model.notification.ContextualNotification;
import org.terracotta.management.model.stats.ContextualStatistics;
import org.terracotta.management.sequence.Sequence;
import org.terracotta.management.service.monitoring.buffer.ReadOnlyBuffer;

/**
 * @author Mathieu Carbou
 */
@CommonComponent
public interface MonitoringService {

  /**
   * Returns an identifier in the M&M topology for an internal voltron client descriptor. This client identifier identifies a client cluster-wise
   */
  ClientIdentifier getClientIdentifier(ClientDescriptor clientDescriptor);

  /**
   * Push a new client-side notification coming from a client descriptor in the monitoring service. This will be put in a best effort-buffer.
   */
  void pushClientNotification(ClientDescriptor from, ContextualNotification notification);

  /**
   * Push some client statistics coming from a client descriptor into the service. This will be put in a best effort-buffer.
   */
  void pushClientStatistics(ClientDescriptor from, ContextualStatistics... statistics);

  /**
   * Associate some tagging information to a client
   */
  void exposeClientTags(ClientDescriptor from, String... tags);

  /**
   * Expose a management registry onto the client identified by a client descriptor
   */
  void exposeClientManagementRegistry(ClientDescriptor caller, ContextContainer contextContainer, Capability... capabilities);

  /**
   * Expose a management registry onto the server entity that is currently consuming this service.
   */
  void exposeServerEntityManagementRegistry(ContextContainer contextContainer, Capability... capabilities);

  /**
   * Push a new server-side notification coming from the entity consuming this service. This will be put in a best effort-buffer.
   */
  void pushServerEntityNotification(ContextualNotification notification);

  /**
   * Push some server-side statistics coming from the entity consuming this service. This will be put in a best effort-buffer.
   */
  void pushServerEntityStatistics(ContextualStatistics... statistics);

  /**
   * @return the current topology. You must not apply any mutation to the returned object.
   * A cluster is a composition of several clients and stripes, but the returned cluster will only have one stripe: the one we are currently on.
   * The stripe name can be configured.
   */
  Cluster readTopology();

  /**
   * @return The server entity identifier of the server entity using this service instance
   */
  ServerEntityIdentifier getServerEntityIdentifier();

  /**
   * Request a management call from an entity client to another client of the same entity
   *
   * @return An unique identifier for this management call
   */
  String sendManagementCallRequest(ClientDescriptor caller, ClientIdentifier to, Context context, String capabilityName, String methodName, Class<?> returnType, Parameter... parameters);

  /**
   * Answer a management call we received and executed
   *
   * @param calledDescriptor         The entity client that was targeted by the call
   * @param from                     The client identifier that was asking the call
   * @param managementCallIdentifier The unique identifier of the management call
   * @param contextualReturn         The result of the call
   */
  void answerManagementCall(ClientDescriptor calledDescriptor, ClientIdentifier from, String managementCallIdentifier, ContextualReturn<?> contextualReturn);

  /**
   * @return The buffer containing all best effort messages, including statistics and notifications.
   */
  ReadOnlyBuffer<Message> createMessageBuffer(int maxBufferSize);

  /**
   * @return The next sequence to use to build a monitoring message
   */
  Sequence nextSequence();
}
