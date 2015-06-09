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
package org.terracotta.management.stats.jackson.mixins.capabilities;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.terracotta.management.capabilities.StatisticsCapability;
import org.terracotta.management.capabilities.descriptors.Descriptor;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * @author Ludovic Orban
 */
public abstract class StatisticsCapabilityMixIn {

  StatisticsCapabilityMixIn(@JsonProperty("name") String name, @JsonProperty("properties") StatisticsCapability.Properties properties, @JsonProperty("descriptions") Collection<Descriptor> descriptions) {
  }

  public static abstract class Properties {
    Properties(@JsonProperty("averageWindowDuration") long averageWindowDuration, @JsonProperty("averageWindowUnit") TimeUnit averageWindowUnit,
               @JsonProperty("historySize") int historySize, @JsonProperty("historyInterval") long historyInterval,
               @JsonProperty("historyIntervalUnit") TimeUnit historyIntervalUnit, @JsonProperty("timeToDisable") long timeToDisable,
               @JsonProperty("timeToDisableUnit") TimeUnit timeToDisableUnit) {
    }
  }

}
