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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.terracotta.management.capabilities.ActionsCapability;
import org.terracotta.management.capabilities.StatisticsCapability;

/**
 * @author Ludovic Orban
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "capabilityType")
@JsonSubTypes({
    @JsonSubTypes.Type(value = ActionsCapability.class, name = "ActionsCapability"),
    @JsonSubTypes.Type(value = StatisticsCapability.class, name = "StatisticsCapability")
})
public abstract class CapabilityMixIn {
}
