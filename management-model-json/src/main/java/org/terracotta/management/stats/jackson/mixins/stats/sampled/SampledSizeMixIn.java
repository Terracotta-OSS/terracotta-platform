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
package org.terracotta.management.stats.jackson.mixins.stats.sampled;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.terracotta.management.stats.MemoryUnit;
import org.terracotta.management.stats.Sample;

import java.util.List;

/**
 * @author Ludovic Orban
 */
public abstract class SampledSizeMixIn {

  SampledSizeMixIn(@JsonProperty("name") String name, @JsonProperty("samples") List<Sample<Long>> value, @JsonProperty("unit") MemoryUnit unit) {
  }

}