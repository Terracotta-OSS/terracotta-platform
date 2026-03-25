/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2026
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
import static org.hamcrest.Matchers.is;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.containsOutput;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.successful;

/**
 * @author Mathieu Carbou
 */
@ClusterDefinition(autoActivate = true, failoverPriority = "")
public class TcPropertiesIT extends DynamicConfigIT {
  @Test
  public void test_can_set_logger_to_warn() {
    assertThat(
        configTool("set", "-connect-to", "localhost:" + getNodePort(), "-setting", "logger-overrides.org.terracotta.dynamic-config.simulate=WARN"),
        is(successful()));
  }

  @Test
  public void test_tc_prop_add_remove() {
    assertThat(
      configTool("set", "-connect-to", "localhost:" + getNodePort(), "-setting", "tc-properties.org.terracotta.SimulationHandler.action=prepare-failure"),
      is(successful()));

    assertThat(
      configTool("set", "-connect-to", "localhost:" + getNodePort(), "-setting", "logger-overrides.org.terracotta.dynamic-config.simulate=WARN"),
      containsOutput("Prepare rejected for node localhost:" + getNodePort() + ". Reason: 'set logger-overrides.org.terracotta.dynamic-config.simulate=WARN': Simulate prepare failure from tc property"));

    assertThat(
      configTool("unset", "-connect-to", "localhost:" + getNodePort(), "-setting", "tc-properties.org.terracotta.SimulationHandler.action"),
      is(successful()));

    assertThat(
      configTool("set", "-connect-to", "localhost:" + getNodePort(), "-setting", "logger-overrides.org.terracotta.dynamic-config.simulate=WARN"),
      is(successful()));
  }
}
