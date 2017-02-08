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
package org.terracotta.management.entity.nms.agent.client;

import org.terracotta.connection.entity.Entity;
import org.terracotta.management.entity.nms.agent.NmsAgent;
import org.terracotta.voltron.proxy.client.EndpointListenerAware;
import org.terracotta.voltron.proxy.client.ServerMessageAware;

/**
 * @author Mathieu Carbou
 */
public interface NmsAgentEntity extends NmsAgent, Entity, ServerMessageAware, EndpointListenerAware {

}
