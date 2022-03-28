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
package org.terracotta.dynamic_config.entity.topology.server;

import org.terracotta.dynamic_config.entity.topology.common.Message;
import org.terracotta.dynamic_config.entity.topology.common.Response;
import org.terracotta.entity.ConfigurationException;
import org.terracotta.entity.PassiveServerEntity;

public class DynamicTopologyPassiveServerEntity implements PassiveServerEntity<Message, Response> {
  @Override
  public void startSyncEntity() {
  }

  @Override
  public void endSyncEntity() {
  }

  @Override
  public void startSyncConcurrencyKey(int concurrencyKey) {
  }

  @Override
  public void endSyncConcurrencyKey(int concurrencyKey) {
  }

  @Override
  public void createNew() throws ConfigurationException {
  }

  @Override
  public void destroy() {
  }
}
