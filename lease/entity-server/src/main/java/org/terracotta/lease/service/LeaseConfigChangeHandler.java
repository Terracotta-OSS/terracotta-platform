/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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

import org.terracotta.common.struct.Measure;
import org.terracotta.common.struct.TimeUnit;
import org.terracotta.dynamic_config.api.model.Configuration;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.Setting;
import org.terracotta.dynamic_config.server.api.ConfigChangeHandler;
import org.terracotta.dynamic_config.server.api.InvalidConfigChangeException;
import org.terracotta.lease.service.config.LeaseConfiguration;

public class LeaseConfigChangeHandler implements ConfigChangeHandler {
  private final LeaseConfiguration leaseConfiguration;

  public LeaseConfigChangeHandler(LeaseConfiguration leaseConfiguration) {
    this.leaseConfiguration = leaseConfiguration;
  }

  @Override
  public void validate(NodeContext nodeContext, Configuration change) throws InvalidConfigChangeException {
    // if the change has no value, this is an unset which rolls back to the default system value
    if (change.hasValue()) {
      try {
        Measure.parse(change.getValue().get(), TimeUnit.class);
      } catch (Exception e) {
        throw new InvalidConfigChangeException(e.toString(), e);
      }
    }
  }

  @Override
  public void apply(Configuration change) {
    Measure<TimeUnit> measure = change.getValue().isPresent() ?
        Measure.parse(change.getValue().get(), TimeUnit.class) :
        Setting.CLIENT_LEASE_DURATION.getDefaultValue();
    long quantity = measure.to(TimeUnit.MILLISECONDS).getQuantity();
    leaseConfiguration.setLeaseLength(quantity);
  }
}
