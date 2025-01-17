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
package org.terracotta.dynamic_config.system_tests.new_options;

import org.junit.Test;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.containsOutput;

@ClusterDefinition(autoActivate = true)
public class GetCommand1x1IT extends DynamicConfigIT {

  @Test
  public void test_getUID() {
    assertThat(configTool("get", "-connect-to", "localhost:" + getNodePort(), "-setting", "cluster-uid"), containsOutput("cluster-uid="));
    assertThat(configTool("get", "-connect-to", "localhost:" + getNodePort(), "-setting", "node-uid"), containsOutput("node-uid="));
    assertThat(configTool("get", "-connect-to", "localhost:" + getNodePort(), "-setting", "stripe-uid"), containsOutput("stripe-uid="));
  }
}
