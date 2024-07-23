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
package org.terracotta.management.registry;

import org.terracotta.management.model.call.Parameter;

import java.util.Collection;
import java.util.Collections;

/**
 * @author Mathieu Carbou
 */
public class DefaultCapabilityManagement implements CapabilityManagement {

  private final String capabilityName;
  private final CapabilityManagementSupport capabilityManagement;

  public DefaultCapabilityManagement(CapabilityManagementSupport capabilityManagement, String capabilityName) {
    this.capabilityManagement = capabilityManagement;
    this.capabilityName = capabilityName;
  }

  @Override
  public StatisticQuery.Builder queryStatistic(String statisticName) {
    return new DefaultStatisticQueryBuilder(capabilityManagement, capabilityName, Collections.singletonList(statisticName));
  }

  @Override
  public StatisticQuery.Builder queryStatistics(Collection<String> statisticNames) {
    return new DefaultStatisticQueryBuilder(capabilityManagement, capabilityName, statisticNames);
  }

  @Override
  public StatisticQuery.Builder queryAllStatistics() {
    return new DefaultStatisticQueryBuilder(capabilityManagement, capabilityName);
  }

  @Override
  public <T> CallQuery.Builder<T> call(String methodName, Class<T> returnType, Parameter... parameters) {
    return new DefaultCallQueryBuilder<>(capabilityManagement, capabilityName, methodName, returnType, parameters);
  }

  @Override
  public CallQuery.Builder<?> call(String methodName, Parameter... parameters) {
    return new DefaultCallQueryBuilder<>(capabilityManagement, capabilityName, methodName, Object.class, parameters);
  }

}
