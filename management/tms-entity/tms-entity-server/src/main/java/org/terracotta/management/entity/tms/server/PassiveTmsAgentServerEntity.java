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

import org.terracotta.management.entity.tms.TmsAgent;
import org.terracotta.voltron.proxy.server.PassiveProxiedServerEntity;

/**
 * @author Mathieu Carbou
 */
class PassiveTmsAgentServerEntity extends PassiveProxiedServerEntity<TmsAgent, Void> {

  private final PassiveTmsAgent tmsAgent;

  PassiveTmsAgentServerEntity(PassiveTmsAgent tmsAgent) {
    super(tmsAgent, null);
    this.tmsAgent = tmsAgent;
  }

  @Override
  public void createNew() {
    tmsAgent.init();
  }

}
