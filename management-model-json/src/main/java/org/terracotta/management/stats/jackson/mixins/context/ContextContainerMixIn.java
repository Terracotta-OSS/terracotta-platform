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
package org.terracotta.management.stats.jackson.mixins.context;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.terracotta.management.context.ContextContainer;

import java.util.Collection;

/**
 * @author Ludovic Orban
 */
public abstract class ContextContainerMixIn {

  ContextContainerMixIn(@JsonProperty("name") String name, @JsonProperty("value") String value, @JsonProperty("subContextContainers") Collection<ContextContainer> subContextContainers) {
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public abstract Collection<ContextContainer> getSubContexts();

}
