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
import org.terracotta.management.model.call.ContextualCall;
import org.terracotta.management.model.call.ContextualReturn;
import org.terracotta.management.model.message.Message;

/**
 * Responsible to execute a management call on a server and set back the result by calling
 * {@link EntityMonitoringService#answerManagementCall(String, ContextualReturn)}.
 * <p>
 * Management call execution can be done directly through the callback, or could also be redirected to the right server
 * with {@link org.terracotta.entity.IEntityMessenger}
 * <p>
 * Also responsible to handle async messaging back to clients. The implementation can choose whether to directly use the
 * {@link org.terracotta.entity.ClientCommunicator} to send messages, or to delay this messages and use the
 * {@link org.terracotta.entity.IEntityMessenger} to callback the entity in a method that will do the messaging
 *
 * @author Mathieu Carbou
 */
@CommonComponent
public interface ManagementExecutor {
  void executeManagementCallOnServer(String managementCallIdentifier, ContextualCall<?> call);

  void sendMessageToClients(Message message);

  void sendMessageToClient(Message message, ClientDescriptor to);
}
