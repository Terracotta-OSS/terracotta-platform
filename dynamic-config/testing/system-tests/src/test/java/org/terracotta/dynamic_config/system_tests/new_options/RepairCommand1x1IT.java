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

import static java.util.Collections.emptyMap;
import static org.hamcrest.CoreMatchers.both;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.containsOutput;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.successful;

@ClusterDefinition(autoActivate = true)
public class RepairCommand1x1IT extends DynamicConfigIT {

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  @Test
  public void test_auto_repair_commit_failure() throws Exception {
    assertThat(
        configTool("set", "-connect-to", "localhost:" + getNodePort(), "-setting", "stripe.1.node.1.logger-overrides.org.terracotta.dynamic-config.simulate=DEBUG"),
        is(both(not(successful())).and(allOf(
            containsOutput("Commit failed for node localhost:" + getNodePort() + ". Reason: Error when applying setting change: 'set logger-overrides.org.terracotta.dynamic-config.simulate=DEBUG (on node UID: "),
            containsOutput("Please run the 'diagnostic' command to diagnose the configuration state and try to run the 'repair' command.")))));

    assertThat(getRuntimeCluster("localhost", getNodePort()).getSingleNode().get().getLoggerOverrides().orDefault(), is(equalTo(emptyMap())));
    assertThat(getUpcomingCluster("localhost", getNodePort()).getSingleNode().get().getLoggerOverrides().orDefault(), is(equalTo(emptyMap())));

    assertThat(
        configTool("set", "-connect-to", "localhost:" + getNodePort(), "-setting", "stripe.1.node.1.logger-overrides.org.terracotta.dynamic-config.simulate=DEBUG"),
        is(both(not(successful())).and(allOf(
            containsOutput("Another change (with UUID "),
            containsOutput(" is already underway on "),
            containsOutput(". It was started by ")
        ))));

    assertThat(
        configTool("repair", "-connect-to", "localhost:" + getNodePort()),
        allOf(
            containsOutput("Repairing configuration by running a commit..."),
            containsOutput("Configuration is repaired")));

    assertThat(getRuntimeCluster("localhost", getNodePort()).getSingleNode().get().getLoggerOverrides().orDefault(), hasEntry("org.terracotta.dynamic-config.simulate", "DEBUG"));
    assertThat(getUpcomingCluster("localhost", getNodePort()).getSingleNode().get().getLoggerOverrides().orDefault(), hasEntry("org.terracotta.dynamic-config.simulate", "DEBUG"));
  }
}
