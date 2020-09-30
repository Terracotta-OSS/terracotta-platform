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
package org.terracotta.dynamic_config.system_tests.new_options;

import org.junit.Test;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.containsOutput;

@ClusterDefinition(autoActivate = true)
public class SetCommand1x1IT extends DynamicConfigIT {
  @Test
  public void testSetLogger() {
    invokeConfigTool("set", "-connect-to", "localhost:" + getNodePort(), "-setting", "stripe.1.node.1.logger-overrides=org.terracotta:TRACE,com.tc:TRACE");

    assertThat(
        invokeConfigTool("get", "-connect-to", "localhost:" + getNodePort(), "-setting", "logger-overrides"),
        containsOutput("logger-overrides=com.tc:TRACE,org.terracotta:TRACE"));

    invokeConfigTool("unset", "-connect-to", "localhost:" + getNodePort(), "-setting", "stripe.1.node.1.logger-overrides.com.tc");

    assertThat(
        invokeConfigTool("get", "-connect-to", "localhost:" + getNodePort(), "-setting", "logger-overrides"),
        containsOutput("logger-overrides=org.terracotta:TRACE"));
  }
}
