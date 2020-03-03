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
package org.terracotta.dynamic_config.xml.plugins;

import org.junit.Test;
import org.terracotta.common.struct.Measure;
import org.terracotta.common.struct.MemoryUnit;
import org.terracotta.offheapresource.config.OffheapResourcesType;
import org.terracotta.offheapresource.config.ResourceType;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class OffheapResourcesTest {

  @Test
  public void testCreateOffheapResourcesType() {
    Map<String, Measure<MemoryUnit>> expected = new HashMap<>();
    expected.put("first", Measure.of(10, MemoryUnit.GB));
    expected.put("second", Measure.of(20, MemoryUnit.MB));

    OffheapResourcesType offheapResourcesType = new OffheapResources(expected).createOffheapResourcesType();

    assertThat(offheapResourcesType, notNullValue());
    Map<String, Measure<MemoryUnit>> actual = new HashMap<>();
    for (ResourceType resourceType : offheapResourcesType.getResource()) {
      actual.put(resourceType.getName(), Measure.of(resourceType.getValue().intValue(),
                                                    MemoryUnit.valueOf(resourceType.getUnit().value())));
    }

    assertThat(actual, is(expected));
  }
}