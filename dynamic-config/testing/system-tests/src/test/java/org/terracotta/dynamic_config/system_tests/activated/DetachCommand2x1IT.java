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

import java.time.Duration;

import static org.junit.Assert.assertThat;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.containsOutput;

/**
 * @author Mathieu Carbou
 */
@ClusterDefinition(stripes = 2, nodesPerStripe = 1, autoActivate = true)
public class DetachCommand2x1IT extends DynamicConfigIT {

  public DetachCommand2x1IT() {
    super(Duration.ofSeconds(180));
  }

  @Test
  public void test_cannot_detach_leading_stripe() {
    assertThat(
        configTool("detach", "-t", "stripe", "-d", "localhost:" + getNodePort(2, 1), "-s", "localhost:" + getNodePort(1, 1)),
        containsOutput("Removing the leading stripe is not allowed"));
  }
}
