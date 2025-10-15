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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.entity.StateDumpCollector;
import org.terracotta.stats.entity.common.Stats;
import org.terracotta.voltron.proxy.server.PassiveProxiedServerEntity;

import java.util.Map;
import java.util.concurrent.Future;

class PassiveStatsServerEntity extends PassiveProxiedServerEntity implements Stats, StatsCallback {

  private static final Logger LOGGER = LoggerFactory.getLogger(PassiveStatsServerEntity.class);

  @Override
  public void destroy() {
    super.destroy();
  }

  @Override
  public void createNew() {
    super.createNew();
    LOGGER.trace("createNew()");
  }

  @Override
  protected void dumpState(StateDumpCollector dump) {
    // No state to dump
  }

  @Override
  public Future<Map<String, Object>> collectCacheStatistics() {
    throw new UnsupportedOperationException("Cannot be called on a passive server");
  }

  @Override
  public Future<Map<String, Object>> collectDatasetStatistics() {
    throw new UnsupportedOperationException("Cannot be called on a passive server");
  }

  @Override
  public Future<Map<String, Object>> collectServerStatistics() {
    throw new UnsupportedOperationException("Cannot be called on a passive server");
  }

}
