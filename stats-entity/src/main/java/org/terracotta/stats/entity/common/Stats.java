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
package org.terracotta.stats.entity.common;

import org.terracotta.voltron.proxy.Async;
import org.terracotta.voltron.proxy.ConcurrencyStrategy;
import org.terracotta.voltron.proxy.ExecutionStrategy;

import java.util.Map;
import java.util.concurrent.Future;

import static org.terracotta.voltron.proxy.ExecutionStrategy.Location.ACTIVE;

/**
 * Stats interface for collecting various statistics
 */
public interface Stats {

  @Async
  @ConcurrencyStrategy(key = ConcurrencyStrategy.UNIVERSAL_KEY)
  @ExecutionStrategy(location = ACTIVE)
  Future<Map<String, Object>> collectCacheStatistics();

  @Async
  @ConcurrencyStrategy(key = ConcurrencyStrategy.UNIVERSAL_KEY)
  @ExecutionStrategy(location = ACTIVE)
  Future<Map<String, Object>> collectDatasetStatistics();

  @Async
  @ConcurrencyStrategy(key = ConcurrencyStrategy.UNIVERSAL_KEY)
  @ExecutionStrategy(location = ACTIVE)
  Future<Map<String, Object>> collectServerStatistics();

}
