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

import org.terracotta.entity.ClientDescriptor;
import org.terracotta.management.model.call.ContextualCall;
import org.terracotta.management.model.message.Message;

/**
 * @author Mathieu Carbou
 */
public class ManagementExecutorAdapter implements ManagementExecutor {
  @Override
  public void executeManagementCallOnServer(String managementCallIdentifier, ContextualCall<?> call) {
    
  }

  @Override
  public void sendMessageToClients(Message message) {

  }

  @Override
  public void sendMessageToClient(Message message, ClientDescriptor to) {

  }
}
