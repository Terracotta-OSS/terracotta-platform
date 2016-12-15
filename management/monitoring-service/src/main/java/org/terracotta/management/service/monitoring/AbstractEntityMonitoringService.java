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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.entity.PlatformConfiguration;

import java.util.Objects;

/**
 * @author Mathieu Carbou
 */
abstract class AbstractEntityMonitoringService implements EntityMonitoringService {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  private final long consumerId;
  private final PlatformConfiguration platformConfiguration;

  AbstractEntityMonitoringService(long consumerId, PlatformConfiguration platformConfiguration) {
    this.consumerId = consumerId;
    this.platformConfiguration = Objects.requireNonNull(platformConfiguration);
  }

  @Override
  public long getConsumerId() {
    return consumerId;
  }

  @Override
  public String getServerName() {
    return platformConfiguration.getServerName();
  }

}
