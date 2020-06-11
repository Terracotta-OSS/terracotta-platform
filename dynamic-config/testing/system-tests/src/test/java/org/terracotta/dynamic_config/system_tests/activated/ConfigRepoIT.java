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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

@ClusterDefinition(nodesPerStripe = 2, autoActivateNodes = {})
public class ConfigRepoIT extends DynamicConfigIT {
  @Test
  public void ensure_created_config_repo_are_the_same_regardless_of_applicability() throws Exception {
    // trigger a change that is applied at runtime
    invokeConfigTool("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.logger-overrides=org.terracotta:TRACE,com.tc:TRACE");

    // config repos written on disk should be the same
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 1)), is(equalTo(getUpcomingCluster("localhost", getNodePort(1, 2)))));
    // runtime topology should be the same
    assertThat(getRuntimeCluster("localhost", getNodePort(1, 1)), is(equalTo(getRuntimeCluster("localhost", getNodePort(1, 2)))));
    // runtime topology should be the same as upcoming one
    assertThat(getRuntimeCluster("localhost", getNodePort(1, 1)), is(equalTo(getUpcomingCluster("localhost", getNodePort(1, 2)))));

    // trigger a change that requires a restart
    invokeConfigTool("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.tc-properties.foo=bar");

    // config repos written on disk should be the same
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 1)), is(equalTo(getUpcomingCluster("localhost", getNodePort(1, 2)))));
    // runtime topology should be the same
    assertThat(getRuntimeCluster("localhost", getNodePort(1, 1)), is(equalTo(getRuntimeCluster("localhost", getNodePort(1, 2)))));
    // runtime topology and upcoming one should be different
    assertThat(getRuntimeCluster("localhost", getNodePort(1, 1)), is(not(equalTo(getUpcomingCluster("localhost", getNodePort(1, 2))))));
  }
}
