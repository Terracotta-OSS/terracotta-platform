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
public class ReplicaSetUnsetCommand2x1IT extends DynamicConfigIT {

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
  public void setFailoverPriority() throws Exception {
    assertThat(
      configTool("set", "-s", "localhost:" + getNodePort(), "-c", "failover-priority=consistency:2"),
      containsOutput("Restart required for cluster"));
    assertThat(
      configTool("get", "-s", "localhost:" + getNodePort(), "-c", "failover-priority"),
      containsOutput("failover-priority=consistency:2"));
  }

  @Test
  public void setNodeLogDir() {
    assertThat(
      configTool("set", "-s", "localhost:" + getNodePort(), "-c", "log-dir=logs/stripe1"),
      containsOutput("Restart required for nodes:"));

    assertThat(
      configTool("get", "-s", "localhost:" + getNodePort(), "-c", "log-dir", "-t", "index"),
      containsOutput("stripe.1.node.1.log-dir=logs/stripe1"));

    assertThat(
      configTool("get", "-s", "localhost:" + getNodePort(), "-r", "-c", "log-dir", "-t", "index"),
      containsOutput("stripe.1.node.1.log-dir=logs"));

    // Restart node and verify that the change has taken effect
    stopNode(1, 1);
    startNode(1, 1);
    waitForPassiveReplicaStart(1, 1);

    assertThat(
      configTool("get", "-s", "localhost:" + getNodePort(), "-r", "-c", "log-dir", "-t", "index"),
      containsOutput("stripe.1.node.1.log-dir=logs/stripe1"));
  }

  @Test
  public void setNodeLogDirWithAutoRestart() {
    assertThat(
      configTool("set", "-connect-to", "localhost:" + getNodePort(), "-setting", "log-dir=logs/stripe1", "-auto-restart"),
      containsOutput("Restart required for nodes:"));

    waitUntil(() -> configTool("get", "-s", "localhost:" + getNodePort(), "-r", "-c", "log-dir", "-t", "index"),
      allOf(is(successful()),
        containsOutput("stripe.1.node.1.log-dir=logs/stripe1")));
  }

  @Test
  public void unsetNodeLogDir() {
    assertThat(
      configTool("set", "-s", "localhost:" + getNodePort(),
        "-c", "stripe.1.node.1.log-dir=a/b",
        "-c", "stripe.2.node.1.log-dir=c/d"),
      is(successful()));

    assertThat(
      configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
      allOf(
        containsOutput("stripe.1.node.1.log-dir=a/b"),
        containsOutput("stripe.2.node.1.log-dir=c/d")
      ));

    assertThat(
      configTool("unset", "-s", "localhost:" + getNodePort(),
        "-c", "stripe.1.node.1.log-dir",
        "-c", "stripe.2.node.1.log-dir"),
      is(successful()));

    assertThat(
      configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
      not(containsOutput("log-dir=")));

    assertThat(
      configTool("set", "-s", "localhost:" + getNodePort(), "-c", "log-dir=e/f"),
      is(successful()));

    assertThat(
      configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
      allOf(
        containsOutput("stripe.1.node.1.log-dir=e/f"),
        containsOutput("stripe.2.node.1.log-dir=e/f")
      ));

    assertThat(
      configTool("unset", "-s", "localhost:" + getNodePort(), "-c", "log-dir"),
      is(successful()));

    assertThat(
      configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
      not(containsOutput("log-dir=")));
  }

  @Test
  public void unsetOffHeapResources() {
    assertThat(configTool("export", "-s", "localhost:" + getNodePort()), is(successful()));

    assertThat(
      configTool("set", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources=boo:64MB,bar:128MB"),
      is(successful()));
    assertThat(
      configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
      containsOutput("offheap-resources=bar\\:128MB,boo\\:64MB,foo\\:1GB,main\\:512MB"));

    // ===
    // IMPORTANT: we do not support to globally replace a map by another map, to prevent any mistake from the user
    // ===
    assertThat(
      configTool("set", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources=boo:128MB,baz:200MB"),
      is(successful()));
    assertThat(
      configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
      containsOutput("offheap-resources=bar\\:128MB,baz\\:200MB,boo\\:128MB,foo\\:1GB,main\\:512MB"));

    // removing a specific property
    assertThat(
      configTool("unset", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.bar"),
      is(not(successful())));
    assertThat(
      configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
      containsOutput("offheap-resources=bar\\:128MB,baz\\:200MB,boo\\:128MB,foo\\:1GB,main\\:512MB"));

    // global removal of a whole map
    assertThat(
      configTool("unset", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources"),
      is(not(successful())));
    assertThat(
      configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
      containsOutput("offheap-resources=bar\\:128MB,baz\\:200MB,boo\\:128MB,foo\\:1GB,main\\:512MB"));
  }

  @Test
  public void unsetTcProperties() {
    assertThat(
      configTool("set", "-s", "localhost:" + getNodePort(), "-c", "tc-properties=foo:1,bar:2"),
      is(successful()));
    assertThat(
      configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
      allOf(
        containsOutput("stripe.1.node.1.tc-properties=bar\\:2,foo\\:1"),
        containsOutput("stripe.2.node.1.tc-properties=bar\\:2,foo\\:1")
      ));

    // ===
    // IMPORTANT: we do not support to globally replace a map by another map, to prevent any mistake from the user
    // ===
    assertThat(
      configTool("set", "-s", "localhost:" + getNodePort(), "-c", "tc-properties=foo:2,baz:3"),
      is(successful()));
    assertThat(
      configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
      allOf(
        containsOutput("stripe.1.node.1.tc-properties=bar\\:2,baz\\:3,foo\\:2"),
        containsOutput("stripe.2.node.1.tc-properties=bar\\:2,baz\\:3,foo\\:2")
      ));

    // removing a specific property
    assertThat(
      configTool("unset", "-s", "localhost:" + getNodePort(),
        "-c", "stripe.1.node.1.tc-properties.bar",
        "-c", "stripe.2.node.1.tc-properties.baz"),
      is(successful()));
    assertThat(
      configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
      allOf(
        containsOutput("stripe.1.node.1.tc-properties=baz\\:3,foo\\:2"),
        containsOutput("stripe.2.node.1.tc-properties=bar\\:2,foo\\:2")
      ));

    // global removal of a whole map
    assertThat(
      configTool("unset", "-s", "localhost:" + getNodePort(),
        "-c", "stripe.1.node.1.tc-properties",
        "-c", "stripe.2.node.1.tc-properties.baz"),
      is(successful()));
    assertThat(
      configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
      allOf(
        not(containsOutput("stripe.1.node.1.tc-properties=")), // this entry is in the output because the user has explicitly set the map to "empty" So it is exported in the config.
        containsOutput("stripe.2.node.1.tc-properties=bar\\:2,foo\\:2")
      ));

    // global removal on cluster
    assertThat(
      configTool("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.tc-properties=bar:2,foo:1"),
      is(successful()));
    assertThat(
      configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
      allOf(
        containsOutput("stripe.1.node.1.tc-properties=bar\\:2,foo\\:1"),
        containsOutput("stripe.2.node.1.tc-properties=bar\\:2,foo\\:2")
      ));
    assertThat(
      configTool("unset", "-s", "localhost:" + getNodePort(), "-c", "tc-properties"),
      is(successful()));
    assertThat(
      configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
      allOf(
        // these entries are in the output because the user has explicitly set the map to "empty" So it is exported in the config.
        not(containsOutput("stripe.1.node.1.tc-properties=")),
        not(containsOutput("stripe.2.node.1.tc-properties="))
      ));
  }

  @Test
  public void unsetSetReplicaProperties() {
    // change properties
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(2, 1),
      "-c", "stripe.1.node.1.relay-hostname=" + "node-1", "-c", "stripe.1.node.1.relay-port=" + "1234", "-c", "stripe.1.node.1.relay-group-port=" + "4567",
      "-c", "stripe.2.node.1.relay-hostname=" + "node-2", "-c", "stripe.2.node.1.relay-port=" + "4567", "-c", "stripe.2.node.1.relay-group-port=" + "1234"),
      allOf(is(successful()), containsOutput("Restart required for nodes:")));

    assertThat(configTool("export", "-s", "localhost:" + getNodePort(1, 1), "-t", "properties", "-r"),
      allOf(containsOutput("stripe.1.node.1.relay-hostname=localhost"), containsOutput("stripe.1.node.1.relay-port=9411"), containsOutput("stripe.1.node.1.relay-group-port=9511")));

    // restart node
    stopNode(1, 1);
    waitForStopped(1, 1);
    startNode(1, 1);
    waitForPassiveReplicaStart(1, 1);

    // config updated at runtime
    assertThat(configTool("export", "-s", "localhost:" + getNodePort(1, 1), "-t", "properties", "-r"),
      allOf(containsOutput("stripe.1.node.1.relay-hostname=node-1"), containsOutput("stripe.1.node.1.relay-port=1234"), containsOutput("stripe.1.node.1.relay-group-port=4567")));

    // unset replica properties
    assertThat(configTool("unset", "-s", "localhost:" + getNodePort(), "-c", "replica"),
      allOf(successful(), not(containsOutput("Restart required for nodes:"))));
    assertThat(configTool("unset", "-s", "localhost:" + getNodePort(),
      "-c", "stripe.1.node.1.relay-hostname", "-c", "stripe.1.node.1.relay-port", "-c", "stripe.1.node.1.relay-group-port",
      "-c", "stripe.2.node.1.relay-hostname", "-c", "stripe.2.node.1.relay-port", "-c", "stripe.2.node.1.relay-group-port"),
      allOf(successful(), containsOutput("Restart required for nodes:")));

    // upcoming cluster
    assertThat(configTool("export", "-s", "localhost:" + getNodePort(1, 1), "-t", "properties"),
      allOf(not(containsOutput("stripe.1.node.1.relay-hostname")), not(containsOutput("stripe.1.node.1.relay-port")), not(containsOutput("stripe.1.node.1.relay-group-port")),
        not(containsOutput("stripe.1.node.2.relay-hostname")), not(containsOutput("stripe.1.node.2.relay-port")), not(containsOutput("stripe.1.node.2.relay-group-port"))));
  }
}
