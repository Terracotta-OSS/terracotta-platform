/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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
package org.terracotta.stats.entity.server;

import org.terracotta.dynamic_config.api.service.TopologyService;
import java.util.concurrent.ScheduledExecutorService;
import org.terracotta.dynamic_config.api.server.DynamicConfigEventService;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.PassiveServerEntity;
import org.terracotta.management.service.monitoring.EntityManagementRegistry;

public class StatsPassiveEntity extends StatsCommonEntity implements PassiveServerEntity<EntityMessage, EntityResponse> {

  StatsPassiveEntity(EntityManagementRegistry managementRegistry, ScheduledExecutorService statsExecutor) {
    super(managementRegistry, statsExecutor);
  }

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
}
