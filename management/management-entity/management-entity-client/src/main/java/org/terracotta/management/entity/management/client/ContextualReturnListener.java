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
package org.terracotta.management.entity.management.client;

import org.terracotta.management.model.call.ContextualReturn;
import org.terracotta.management.model.call.Parameter;
import org.terracotta.management.model.cluster.ClientIdentifier;
import org.terracotta.management.model.context.Context;

/**
 * @author Mathieu Carbou
 */
public interface ContextualReturnListener {

  /**
   * Listens to management call results
   *
   * @param from             the target of the management call
   * @param managementCallId the identifier of the management call, return by {@link ManagementAgentService#call(ClientIdentifier, Context, String, String, Class, Parameter...)}
   * @param aReturn          the returned result
   */
  void onContextualReturn(ClientIdentifier from, String managementCallId, ContextualReturn<?> aReturn);

}
