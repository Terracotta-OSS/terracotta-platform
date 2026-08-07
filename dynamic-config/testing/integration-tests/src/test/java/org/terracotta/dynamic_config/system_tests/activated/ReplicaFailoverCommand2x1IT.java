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

import org.junit.Before;
import org.junit.Test;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.containsOutput;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.successful;

@ClusterDefinition(stripes = 2, autoStart = false)
public class ReplicaFailoverCommand2x1IT extends DynamicConfigIT {
  @Before
  public void before() throws Exception {
    startNode(1, 1);
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(1, 1), "-c", "replica=true", "-c", "stripe.1.node.1.relay-hostname=" + "localhost", "-c", "stripe.1.node.1.relay-port=" + "9411", "-c", "stripe.1.node.1.relay-group-port=" + "9511"), is(successful()));
    startNode(2, 1);
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(2, 1), "-c", "replica=true", "-c", "stripe.1.node.1.relay-hostname=" + "localhost", "-c", "stripe.1.node.1.relay-port=" + "9412", "-c", "stripe.1.node.1.relay-group-port=" + "9512"), is(successful()));
    assertThat(configTool("attach", "-t", "stripe", "-d", "localhost:" + getNodePort(1, 1), "-s", "localhost:" + getNodePort(2, 1)), is(successful()));
    activateCluster();
    waitForPassiveReplicaStart(1, 1);
    waitForPassiveReplicaStart(2, 1);
  }

  @Test
  public void testUnsetReplicaFailover() {
    assertThat(configTool("replica-failover", "-connect-to", "localhost:" + getNodePort()), is(successful()));
    assertThat(configTool("get", "-s", "localhost:" + getNodePort(1, 1), "-c", "replica"), containsOutput("replica=false"));
  }

  @Test
  public void testUnsetReplicaFailoverWithPartialPrepareFailure() {
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(1, 1), "-c", "stripe.1.node.1.tc-properties.org.terracotta.ReplicaSimulationHandler.action=prepare-failure"), is(successful()));
    assertThat(configTool("replica-failover", "-connect-to", "localhost:" + getNodePort()), containsOutput("Two-Phase commit failed"));
    assertThat(configTool("get", "-s", "localhost:" + getNodePort(1, 1), "-c", "replica"), containsOutput("replica=true"));
    assertThat(configTool("unset", "-connect-to", "localhost:" + getNodePort(), "-setting", "tc-properties.org.terracotta.ReplicaSimulationHandler.action"), is(successful()));
    assertThat(configTool("replica-failover", "-connect-to", "localhost:" + getNodePort()), is(successful()));
    assertThat(configTool("get", "-s", "localhost:" + getNodePort(1, 1), "-c", "replica"), containsOutput("replica=false"));
  }

  @Test
  public void testUnsetReplicaFailoverWithCommitFailureAllNodesAndRepair() {
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(1, 1), "-c", "tc-properties.org.terracotta.ReplicaSimulationHandler.action=commit-failure"), is(successful()));
    assertThat(configTool("replica-failover", "-connect-to", "localhost:" + getNodePort()),
      allOf(not(successful()),
        containsOutput("Commit failed for node"), containsOutput("run the 'repair' command")));
    assertThat(configTool("repair", "-s", "localhost:" + getNodePort(1, 1)),
      allOf(containsOutput("Repairing configuration by running a commit"),
        containsOutput("Configuration is repaired")));
    assertThat(configTool("get", "-s", "localhost:" + getNodePort(1, 1), "-c", "replica"), containsOutput("replica=false"));
  }

  @Test
  public void unsetReplicaCommitFailureOneNodeAndRepair() {
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(1, 1), "-c", "stripe.2.node.1.tc-properties.org.terracotta.ReplicaSimulationHandler.action=commit-failure"), is(successful()));
    assertThat(configTool("replica-failover", "-connect-to", "localhost:" + getNodePort()),
      allOf(not(successful()),
        containsOutput("Commit failed for node localhost:" + getNodePort(2, 1)),
        containsOutput("run the 'repair' command")));
    assertThat(configTool("repair", "-f", "commit", "-s", "localhost:" + getNodePort(1, 1)),
      allOf(containsOutput("Repairing configuration by running a commit"),
        containsOutput("Configuration is repaired")));
    assertThat(configTool("get", "-s", "localhost:" + getNodePort(1, 1), "-c", "replica"), containsOutput("replica=false"));
  }

  @Test
  public void rollbackFailure_requiresRepair() {
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(1, 1), "-c", "stripe.1.node.1.tc-properties.org.terracotta.ReplicaSimulationHandler.action=rollback-failure"), is(successful()));
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(2, 1), "-c", "stripe.2.node.1.tc-properties.org.terracotta.ReplicaSimulationHandler.action=prepare-failure"), is(successful()));
    assertThat(configTool("replica-failover", "-connect-to", "localhost:" + getNodePort()),
      allOf(not(successful()),
        containsOutput("Two-Phase commit failed")));
    assertThat(configTool("repair", "-f", "rollback", "-s", "localhost:" + getNodePort()),
      allOf(containsOutput("Repairing configuration by running a rollback"),
        containsOutput("Configuration is repaired")));
    assertThat(configTool("get", "-s", "localhost:" + getNodePort(), "-c", "replica"), containsOutput("replica=true"));
  }
}
