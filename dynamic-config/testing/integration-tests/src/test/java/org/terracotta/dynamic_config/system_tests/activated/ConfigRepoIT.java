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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.successful;

@ClusterDefinition(nodesPerStripe = 2, autoActivate = true)
public class ConfigRepoIT extends DynamicConfigIT {
  @Test
  public void ensure_created_config_repo_are_the_same_regardless_of_applicability() throws Exception {
    // trigger a change that is applied at runtime
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.logger-overrides=org.terracotta:TRACE,com.tc:TRACE"), is(successful()));

    // config repos written on disk should be the same
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 1)), is(equalTo(getUpcomingCluster("localhost", getNodePort(1, 2)))));
    // runtime topology should be the same
    assertThat(getRuntimeCluster("localhost", getNodePort(1, 1)), is(equalTo(getRuntimeCluster("localhost", getNodePort(1, 2)))));
    // runtime topology should be the same as upcoming one
    assertThat(getRuntimeCluster("localhost", getNodePort(1, 1)), is(equalTo(getUpcomingCluster("localhost", getNodePort(1, 2)))));

    // trigger a change that requires a restart on a specific node
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.tc-properties.foo=bar"), is(successful()));

    // config repos written on disk should be the same
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 1)), is(equalTo(getUpcomingCluster("localhost", getNodePort(1, 2)))));
    // runtime topology should not be the same because node 1 needs a restart
    assertThat(getRuntimeCluster("localhost", getNodePort(1, 1)), is(not(equalTo(getRuntimeCluster("localhost", getNodePort(1, 2))))));

    // runtime topology and upcoming one should be different on node 1
    assertThat(getRuntimeCluster("localhost", getNodePort(1, 1)), is(not(equalTo(getUpcomingCluster("localhost", getNodePort(1, 1))))));
    // runtime topology and upcoming one should be the same on node 2
    assertThat(getRuntimeCluster("localhost", getNodePort(1, 2)), is(equalTo(getUpcomingCluster("localhost", getNodePort(1, 2)))));
  }
}
