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
package org.terracotta.dynamic_config.system_tests.activated;

import org.junit.Test;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.containsOutput;

@ClusterDefinition(autoActivate = true)
public class GetCommand1x1IT extends DynamicConfigIT {

  @Test
  public void test_getUID() {
    assertThat(invokeConfigTool("get", "-s", "localhost:" + getNodePort(), "-c", "cluster-uid"), containsOutput("cluster-uid="));
    assertThat(invokeConfigTool("get", "-s", "localhost:" + getNodePort(), "-c", "node-uid"), containsOutput("node-uid="));
    assertThat(invokeConfigTool("get", "-s", "localhost:" + getNodePort(), "-c", "stripe-uid"), containsOutput("stripe-uid="));
  }
}
