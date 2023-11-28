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
package org.terracotta.management.registry;

import org.terracotta.management.model.context.Context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * @author Mathieu Carbou
 */
public class DefaultStatisticQueryBuilder implements StatisticQuery.Builder {

  private final CapabilityManagementSupport capabilityManagement;
  private final String capabilityName;
  private final Collection<String> statisticNames;
  private final Collection<Context> contexts;
  private final long since;

  DefaultStatisticQueryBuilder(CapabilityManagementSupport capabilityManagement, String capabilityName, Collection<String> statisticNames) {
    this(capabilityManagement, capabilityName, statisticNames, Collections.<Context>emptyList(), 0L);
  }

  DefaultStatisticQueryBuilder(CapabilityManagementSupport capabilityManagement, String capabilityName) {
    this(capabilityManagement, capabilityName, Collections.<String>emptyList(), Collections.<Context>emptyList(), 0L);
  }

  private DefaultStatisticQueryBuilder(CapabilityManagementSupport capabilityManagement, String capabilityName, Collection<String> statisticNames, Collection<Context> contexts, long since) {
    this.capabilityManagement = capabilityManagement;
    this.capabilityName = capabilityName;
    this.statisticNames = new LinkedHashSet<>(statisticNames);
    this.contexts = contexts;
    this.since = since;
  }

  @Override
  public StatisticQuery build() {
    return new DefaultStatisticQuery(capabilityManagement, capabilityName, statisticNames, contexts, since);
  }

  @Override
  public StatisticQuery.Builder on(Context context) {
    if (!contexts.contains(context)) {
      List<Context> contexts = new ArrayList<>(this.contexts);
      contexts.add(context);
      return new DefaultStatisticQueryBuilder(capabilityManagement, capabilityName, statisticNames, contexts, since);
    }
    return this;
  }

  @Override
  public StatisticQuery.Builder on(Collection<? extends Context> contexts) {
    StatisticQuery.Builder newBuilder = this;
    for (Context context : contexts) {
      newBuilder = newBuilder.on(context);
    }
    return newBuilder;
  }

  @Override
  public StatisticQuery.Builder since(long unixTimestampMs) {
    return new DefaultStatisticQueryBuilder(capabilityManagement, capabilityName, statisticNames, contexts, unixTimestampMs);
  }

}
