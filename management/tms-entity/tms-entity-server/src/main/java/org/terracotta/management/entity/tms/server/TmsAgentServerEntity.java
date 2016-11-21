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

import org.terracotta.entity.ClientDescriptor;
import org.terracotta.management.entity.tms.TmsAgent;
import org.terracotta.management.entity.tms.TmsAgentConfig;
import org.terracotta.voltron.proxy.server.ProxiedServerEntity;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Mathieu Carbou
 */
class TmsAgentServerEntity extends ProxiedServerEntity<TmsAgent> {

  private final AtomicBoolean connected  = new AtomicBoolean();
  private final TmsAgentImpl tmsAgent;

  TmsAgentServerEntity(TmsAgentImpl tmsAgent) {
    super(tmsAgent);
    this.tmsAgent = tmsAgent;
  }

  @Override
  public void createNew() {
    tmsAgent.init();
  }

  @Override
  public void destroy() {
    tmsAgent.close();
    super.destroy();
  }

  @Override
  public void connected(ClientDescriptor clientDescriptor) {
    super.connected(clientDescriptor);
    if(!connected.compareAndSet(false, true)) {
      throw new AssertionError("Only one connection allowed per " + TmsAgentConfig.ENTITY_TYPE);
    }
  }

  @Override
  public void disconnected(ClientDescriptor clientDescriptor) {
    connected.set(false);
    super.disconnected(clientDescriptor);
  }

}
