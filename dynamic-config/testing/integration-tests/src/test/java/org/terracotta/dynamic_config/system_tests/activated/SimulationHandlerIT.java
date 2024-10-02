/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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

/**
 * @author Mathieu Carbou
 */
@ClusterDefinition(autoActivate = true, failoverPriority = "")
public class SimulationHandlerIT extends DynamicConfigIT {
  @Test
  public void test_missing_value() {
    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.logger-overrides.org.terracotta.dynamic-config.simulate="),
        containsOutput("Invalid input: 'stripe.1.node.1.logger-overrides.org.terracotta.dynamic-config.simulate='. Reason: Operation set requires a value"));
  }

  @Test
  public void test_prepare_fails() {
    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.logger-overrides.org.terracotta.dynamic-config.simulate=TRACE"),
        containsOutput("Prepare rejected for node localhost:" + getNodePort() + ". Reason: 'set logger-overrides.org.terracotta.dynamic-config.simulate=TRACE (on node UID: "));
  }

  @Test
  public void test_commit_fails_permanently() {
    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.logger-overrides.org.terracotta.dynamic-config.simulate=INFO"),
        containsOutput("Reason: Error when applying setting change: 'set logger-overrides.org.terracotta.dynamic-config.simulate=INFO (on node UID: "));
  }

  @Test
  public void test_commit_fails_temporary() {
    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.logger-overrides.org.terracotta.dynamic-config.simulate=DEBUG"),
        containsOutput("Reason: Error when applying setting change: 'set logger-overrides.org.terracotta.dynamic-config.simulate=DEBUG (on node UID: "));
  }
}
