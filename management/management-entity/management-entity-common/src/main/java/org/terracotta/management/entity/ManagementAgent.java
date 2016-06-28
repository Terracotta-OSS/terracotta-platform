/**
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
package org.terracotta.management.entity;

import org.terracotta.management.model.capabilities.Capability;
import org.terracotta.management.model.cluster.ClientIdentifier;
import org.terracotta.management.model.context.ContextContainer;
import org.terracotta.voltron.proxy.Async;
import org.terracotta.voltron.proxy.ClientId;

import java.util.Collection;
import java.util.concurrent.Future;

/**
 * @author Mathieu Carbou
 */
public interface ManagementAgent {

  /**
   * Exposes this management registry output (context container and capabilities) over this connection.
   *
   * @param contextContainer output from Management registry
   * @param capabilities     output from Management registry
   * @param clientDescriptor must be null, used only for implementation
   */
  @Async(Async.Ack.NONE)
  Future<Void> exposeManagementMetadata(@ClientId Object clientDescriptor, ContextContainer contextContainer, Capability[] capabilities);

  /**
   * Exposes client tags
   *
   * @param clientDescriptor must be null, used only for implementation
   * @param tags             the tags to expose for this client
   */
  @Async(Async.Ack.NONE)
  Future<Void> exposeTags(@ClientId Object clientDescriptor, String... tags);

  /**
   * Gets the {@link ClientIdentifier} for the underlying logical connection.
   *
   * @param clientDescriptor must be null, used only for implementation
   */
  @Async(Async.Ack.NONE)
  Future<ClientIdentifier> getClientIdentifier(@ClientId Object clientDescriptor);

}
