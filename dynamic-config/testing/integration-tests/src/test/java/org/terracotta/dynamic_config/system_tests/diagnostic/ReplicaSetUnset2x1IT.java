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

@ClusterDefinition(stripes = 2)
public class ReplicaSetUnset2x1IT extends DynamicConfigIT {
  @Before
  public void setup() throws Exception {
    assertThat(configTool("attach", "-to-cluster", "localhost:" + getNodePort(1, 1), "-stripe", "localhost:" + getNodePort(2, 1)), is(successful()));
  }

  @Test
  public void setReplica() {
    String node1 = getNodeName(1, 1);
    String node2 = getNodeName(2, 1);

    // replica properties missing on one node
    assertThat(configTool("set", "-connect-to", "localhost:" + getNodePort(), "-setting", "replica=" + "true",
        "-setting", node1 + ":relay-hostname=" + "localhost1", "-setting", node1 + ":relay-port=" + "9411", "-setting", node1 + ":relay-group-port=" + "9411"),
      allOf(
        is(not(successful())),
        containsOutput("The replica setting is enabled for node with name: node-2-1, replica properties: {relay-hostname=null, relay-port=null, relay-group-port=null} aren't well-formed, [relay-hostname, relay-port, relay-group-port] need to be set together"))
    );

    // all nodes will need set together at once
    assertThat(configTool("set", "-connect-to", "localhost:" + getNodePort(), "-setting", "replica=" + "true",
        "-setting", node1 + ":relay-hostname=" + "localhost1", "-setting", node1 + ":relay-port=" + "9411", "-setting", node1 + ":relay-group-port=" + "9411",
        "-setting", node2 + ":relay-hostname=" + "localhost1", "-setting", node2 + ":relay-port=" + "9411", "-setting", node2 + ":relay-group-port=" + "9411"
      ),
      is(successful()));

    // export
    assertThat(configTool("export", "-s", "localhost:" + getNodePort()),
      allOf(is(successful()),
        containsOutput("replica=true"),
        containsOutput("node-1-1:relay-hostname=localhost"),
        containsOutput("node-1-1:relay-port=9411"),
        containsOutput("node-1-1:relay-group-port=9411"),
        containsOutput("node-2-1:relay-hostname=localhost"),
        containsOutput("node-2-1:relay-port=9411"),
        containsOutput("node-2-1:relay-group-port=9411")
      ));
  }

  @Test
  public void setUnsetReplica() {
    String node1 = getNodeName(1, 1);
    String node2 = getNodeName(2, 1);
    assertThat(configTool("set", "-connect-to", "localhost:" + getNodePort(), "-setting", "replica=" + "true",
        "-setting", node1 + ":relay-hostname=" + "localhost1", "-setting", node1 + ":relay-port=" + "9411", "-setting", node1 + ":relay-group-port=" + "9411",
        "-setting", node2 + ":relay-hostname=" + "localhost1", "-setting", node2 + ":relay-port=" + "9411", "-setting", node2 + ":relay-group-port=" + "9411"
      ),
      is(successful()));

    // unset replica flag
    assertThat(configTool("unset", "-connect-to", "localhost:" + getNodePort(), "-setting", "replica"), is((successful())));
    // replica dependent properties are still set
    assertThat(
      configTool("export", "-s", "localhost:" + getNodePort()),
      allOf(
        // doesn't output the property if explicitly unset
        not(containsOutput("replica=false")),
        containsOutput("node-1-1:relay-hostname=localhost"), containsOutput("node-1-1:relay-port=9411"), containsOutput("node-1-1:relay-group-port=9411"),
        containsOutput("node-2-1:relay-hostname=localhost"), containsOutput("node-2-1:relay-port=9411"), containsOutput("node-2-1:relay-group-port=9411")
      ));

    // set replica again
    assertThat(configTool("set", "-connect-to", "localhost:" + getNodePort(), "-setting", "replica=" + "true"), is(successful()));

    // unset partial dependent properties on a node
    assertThat(configTool("unset", "-connect-to", "localhost:" + getNodePort(),
        "-setting", "replica",
        "-setting", node1 + ":relay-hostname"),
      allOf(is(not(successful())),
        containsOutput("The replica setting is disabled for node with name: node-1-1, properties: {relay-hostname=null, relay-port=9411, relay-group-port=9411} are partially configured. Either remove all properties or set all required properties"))
    );

    // unset dependent properties on a single node
    assertThat(configTool("unset", "-connect-to", "localhost:" + getNodePort(),
        "-setting", "replica",
        "-setting", node1 + ":relay-hostname", "-setting", node1 + ":relay-port", "-setting", node1 + ":relay-group-port"),
      allOf(is(successful())));
    assertThat(
      configTool("export", "-s", "localhost:" + getNodePort()),
      allOf(
        not(containsOutput("node-1-1:relay-hostname=localhost")),
        containsOutput("node-2-1:relay-hostname=localhost")
      ));

    // unset dependent properties on 2nd node
    assertThat(configTool("unset", "-connect-to", "localhost:" + getNodePort(),
        "-setting", node2 + ":relay-hostname", "-setting", node2 + ":relay-port", "-setting", node2 + ":relay-group-port"),
      allOf(is(successful())));
    // no replica properties
    assertThat(
      configTool("export", "-s", "localhost:" + getNodePort()),
      allOf(
        not(containsOutput("node-1-1:relay-hostname=localhost")),
        not(containsOutput("node-2-1:relay-hostname=localhost")), not(containsOutput("node-2-1:relay-port=9411")), not(containsOutput("node-2-1:relay-group-port=9411"))
      ));
  }
}
