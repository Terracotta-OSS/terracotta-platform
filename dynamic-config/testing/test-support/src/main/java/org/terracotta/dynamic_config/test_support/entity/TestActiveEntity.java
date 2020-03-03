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
package org.terracotta.dynamic_config.test_support.entity;

import org.terracotta.entity.ActiveServerEntity;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.PassiveSynchronizationChannel;

public class TestActiveEntity extends TestCommonEntity implements ActiveServerEntity<EntityMessage, EntityResponse> {
  @Override
  public void connected(ClientDescriptor clientDescriptor) {
    throw new AssertionError("Client not allowed to connect to this entity");
  }

  @Override
  public void disconnected(ClientDescriptor clientDescriptor) {
    throw new AssertionError("Client not allowed to connect to this entity");
  }

  @Override
  public void loadExisting() {

  }

  @Override
  public void synchronizeKeyToPassive(PassiveSynchronizationChannel<EntityMessage> syncChannel, int concurrencyKey) {

  }

  @Override
  public ReconnectHandler startReconnect() {
    return null;
  }
}
