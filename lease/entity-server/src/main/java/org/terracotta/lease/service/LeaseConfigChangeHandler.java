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
package org.terracotta.lease.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.common.struct.Measure;
import org.terracotta.common.struct.TimeUnit;
import org.terracotta.dynamic_config.api.model.Configuration;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.service.ConfigChangeHandler;
import org.terracotta.dynamic_config.api.service.InvalidConfigChangeException;

import java.time.Duration;

import static org.terracotta.lease.service.config.LeaseConstants.DEFAULT_LEASE_LENGTH;
import static org.terracotta.lease.service.config.LeaseConstants.MAX_LEASE_LENGTH;

public class LeaseConfigChangeHandler implements ConfigChangeHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(LeaseConfigChangeHandler.class);

  private final LeaseDuration leaseDuration;

  public LeaseConfigChangeHandler(LeaseDuration leaseDuration) {
    this.leaseDuration = leaseDuration;
  }

  @Override
  public void validate(NodeContext nodeContext, Configuration change) throws InvalidConfigChangeException {
    try {
      Measure.parse(change.getValue(), TimeUnit.class);
    } catch (Exception e) {
      throw new InvalidConfigChangeException(e.getMessage(), e);
    }
  }

  @Override
  public void apply(Configuration change) {
    Measure<TimeUnit> measure = Measure.parse(change.getValue(), TimeUnit.class);
    long quantity = measure.to(TimeUnit.MILLISECONDS).getQuantity();
    if (quantity <= 0) {
      LOGGER.warn("Non-positive lease length: {}, ignoring it", quantity);
      quantity = DEFAULT_LEASE_LENGTH;
    }
    if (quantity > MAX_LEASE_LENGTH) {
      LOGGER.warn("Excessive lease length: {}, using smaller value: {}", quantity, MAX_LEASE_LENGTH);
      quantity = MAX_LEASE_LENGTH;
    }
    LOGGER.info("Updating lease length of {} ms", quantity);
    leaseDuration.set(Duration.ofMillis(quantity));
  }
}
