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
package org.terracotta.dynamic_config.system_tests.diagnostic;

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

@ClusterDefinition(autoStart = false)
public class ReplicaGetSetUnset1x1IT extends DynamicConfigIT {
  @Before
  public void setup() throws Exception {
    startNode(1, 1, getNewOptions(getNode(1, 1), "-replica", "true", "-relay-hostname", "localhost", "-relay-port", "9410", "-relay-group-port", "9430"));
    waitForPassiveRelay(1, 1);
  }

  @Test
  public void testGetAllowedReplica() {
    assertThat(configTool("get", "-s", "localhost:" + getNodePort(), "-c", "replica"), allOf(is(successful()), containsOutput("node-1-1:replica=true")));
  }

  @Test
  public void testExportReplica() {
    assertThat(configTool("export", "-s", "localhost:" + getNodePort()), allOf(is(successful()), containsOutput("node-1-1:replica=true")));
  }

  @Test
  public void testSetOperationNotAllowedReplica() {
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(), "-c", "tc-properties=foo:foo"),
      allOf(is(not(successful())), containsOutput("Node with name: node-1-1 has the replica setting enabled. SET operation is not supported on replica node")));
  }

  @Test
  public void testUnsetOperationNotAllowedReplica() {
    assertThat(configTool("unset", "-s", "localhost:" + getNodePort(), "-c", "log-dir"),
      allOf(is(not(successful())), containsOutput("Node with name: node-1-1 has the replica setting enabled. UNSET operation is not supported on replica node")));
  }
}
