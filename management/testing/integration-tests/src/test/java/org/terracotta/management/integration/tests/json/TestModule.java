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
package org.terracotta.management.integration.tests.json;

import com.google.gson.annotations.JsonAdapter;
import org.terracotta.json.gson.GsonConfig;
import org.terracotta.json.gson.GsonModule;
import org.terracotta.json.gson.RuntimeTypeAdapterFactory;
import org.terracotta.management.model.capabilities.Capability;
import org.terracotta.management.model.capabilities.descriptors.Descriptor;
import org.terracotta.management.model.context.Contextual;
import org.terracotta.statistics.Sample;
import org.terracotta.statistics.ValueStatistic;

import java.io.Serializable;

/**
 * @author Mathieu Carbou
 */
public class TestModule implements GsonModule {
  @Override
  public void configure(GsonConfig config) {
    config.registerMixin(Sample.class, SampleMixin.class);
    config.serializeSubtypes(Capability.class);
    config.serializeSubtypes(Descriptor.class);
    config.serializeSubtypes(Contextual.class);
    config.serializeSubtypes(ValueStatistic.class);
  }

  static class SampleMixin<T extends Serializable> {
    @JsonAdapter(RuntimeTypeAdapterFactory.class)
    private T sample;
  }
}
