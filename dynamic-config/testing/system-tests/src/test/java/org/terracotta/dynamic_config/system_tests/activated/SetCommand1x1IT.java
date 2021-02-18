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

import com.terracotta.connection.api.TerracottaConnectionService;
import org.junit.Test;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Configuration;
import org.terracotta.dynamic_config.entity.topology.client.DynamicTopologyEntity;
import org.terracotta.dynamic_config.entity.topology.client.DynamicTopologyEntityFactory;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.containsOutput;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.successful;

@ClusterDefinition(autoActivate = true)
public class SetCommand1x1IT extends DynamicConfigIT {
  @Test
  public void setOffheapResource_decreaseSize() {
    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.main=1MB"),
        containsOutput("should be larger than the old size"));
  }

  @Test
  public void setOffheapResource_increaseSize() {
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.main=1GB"), is(successful()));

    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.main"),
        containsOutput("offheap-resources.main=1GB"));
  }

  @Test
  public void setOffheapResource_addResource() {
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources=second:1GB"), is(successful()));

    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.second"),
        containsOutput("offheap-resources.second=1GB"));
  }

  @Test
  public void addOffheapResource_with_decimal_value() {
    assertThat(
        configTool("-v", "set", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources=main2:1.5MB"),
        containsOutput("Invalid input: 'offheap-resources.main2=1.5MB'. Reason: offheap-resources.main2 is invalid: Invalid measure: '1.5'. <quantity> must be a positive integer."));
  }

  @Test
  public void setOffheapResources_addResources() {
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.second=1GB", "-c", "offheap-resources.third=1GB"), is(successful()));

    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.second", "-c", "offheap-resources.third"),
        allOf(containsOutput("offheap-resources.second=1GB"), containsOutput("offheap-resources.third=1GB")));
  }

  @Test
  public void setOffheapResources_addResources_sane_new_keys_and_lower_value() {
    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.foo=1GB", "-c", "offheap-resources.foo=500MB"),
        containsOutput("Duplicate configurations found: offheap-resources.foo=1GB and offheap-resources.foo=500MB"));
  }

  @Test
  public void setOffheapResources_addResource_increaseSize() {
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources=main:1GB,second:1GB"), is(successful()));

    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources"),
        containsOutput("offheap-resources=foo:1GB,main:1GB,second:1GB"));
  }

  @Test
  public void setOffheapResources_newResource_decreaseSize() {
    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.second=1GB", "-c", "offheap-resources.main=1MB"),
        containsOutput("should be larger than the old size"));
  }

  @Test
  public void setFailoverPriority_Consistency() {
    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(), "-c", "failover-priority=consistency:2"),
        containsOutput("restart of the cluster is required"));

    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-c", "failover-priority"),
        containsOutput("failover-priority=consistency:2"));
  }

  @Test
  public void setClusterAndNodeRestartRequiringChangesInOneCommand() {
    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(), "-c", "failover-priority=consistency:2", "-c", "log-dir=new-logs"),
        allOf(containsOutput("restart of the cluster is required"), not(containsOutput("restart of nodes"))));

    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-c", "failover-priority", "-c", "log-dir"),
        allOf(containsOutput("failover-priority=consistency:2"), containsOutput("stripe.1.node.1.log-dir=new-logs")));
  }

  @Test
  public void setClusterAndNoRestartRequiringChangesInOneCommand() {
    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(), "-c", "failover-priority=consistency:2", "-c", "cluster-name=new-cluster"),
        allOf(containsOutput("restart of the cluster is required"), not(containsOutput("restart of nodes"))));

    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-c", "failover-priority", "-c", "cluster-name"),
        allOf(containsOutput("failover-priority=consistency:2"), containsOutput("cluster-name=new-cluster")));
  }

  @Test
  public void setNodeAndNoRestartRequiringChangesInOneCommand() {
    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(), "-c", "log-dir=new-logs", "-c", "cluster-name=new-cluster"),
        allOf(not(containsOutput("restart of the cluster is required")), containsOutput("restart of nodes")));

    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-c", "cluster-name", "-c", "log-dir"),
        allOf(containsOutput("cluster-name=new-cluster"), containsOutput("stripe.1.node.1.log-dir=new-logs")));
  }

  @Test
  public void setNodeLogDir() {
    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(), "-c", "log-dir=logs/stripe1"),
        containsOutput("IMPORTANT: A restart of nodes: node-1-1 is required to apply the changes"));

    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-c", "log-dir"),
        containsOutput("stripe.1.node.1.log-dir=logs/stripe1"));

    // Restart node and verify that the change has taken effect
    stopNode(1, 1);
    startNode(1, 1);

    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-r", "-c", "log-dir"),
        containsOutput("stripe.1.node.1.log-dir=logs/stripe1"));
  }

  @Test
  public void setNodeLogDir_pathAtDestinationIsAFile() throws Exception {
    File toUpload = new File(SetCommand1x1IT.class.getResource("/dummy.txt").toURI());
    angela.tsa().browse(getNode(1, 1), ".").upload(toUpload);
    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(), "-c", "log-dir=dummy.txt"),
        containsOutput("dummy.txt exists, but is not a directory"));
  }

  @Test
  public void setNodeBindAddress() {
    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.bind-address=127.0.0.1"),
        containsOutput("Reason: Setting 'bind-address' cannot be set when node is activated"));
  }

  @Test
  public void setNodeName() {
    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.name=foo"),
        containsOutput("Error: Invalid input: 'stripe.1.node.1.name=foo'. Reason: Setting 'name' cannot be set when node is activated"));
  }

  @Test
  public void setNodeGroupPort() {
    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.group-port=1024"),
        containsOutput("Reason: Setting 'group-port' cannot be set when node is activated"));
  }

  @Test
  public void setNodeGroupBindAddress() {
    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.group-bind-address=127.0.0.1"),
        containsOutput("Reason: Setting 'group-bind-address' cannot be set when node is activated"));
  }

  @Test
  public void testTcProperty() {
    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(), "-c", "tc-properties.foo=bar"),
        containsOutput("IMPORTANT: A restart of the cluster is required to apply the changes"));

    assertThat(
        configTool("get", "-r", "-s", "localhost:" + getNodePort(), "-c", "tc-properties"),
        not(containsOutput("tc-properties=foo:bar")));

    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-c", "tc-properties"),
        containsOutput("tc-properties=foo:bar"));

    assertThat(configTool("unset", "-s", "localhost:" + getNodePort(), "-c", "tc-properties.foo"), is(successful()));

    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-c", "tc-properties"),
        not(containsOutput("tc-properties=foo:bar")));
  }

  @Test
  public void testSetLogger() {
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.logger-overrides=org.terracotta:TRACE,com.tc:TRACE"), is(successful()));

    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-c", "logger-overrides"),
        containsOutput("logger-overrides=com.tc:TRACE,org.terracotta:TRACE"));

    assertThat(configTool("unset", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.logger-overrides.com.tc"), is(successful()));

    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-c", "logger-overrides"),
        containsOutput("logger-overrides=org.terracotta:TRACE"));
  }

  @Test
  public void change_cluster_name_back() throws Exception {
    // TDB-5067
    String clusterName = usingTopologyService(1, 1, topologyService -> topologyService.getUpcomingNodeContext().getCluster().getName());
    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(), "-c", "cluster-name=new-name"),
        not(containsOutput("IMPORTANT: A restart of the cluster is required to apply the changes")));
    assertThat(configTool("diagnostic", "-s", "localhost:" + getNodePort(1, 1)),
        allOf(containsOutput("Node restart required: NO"), containsOutput("Node last configuration change details: set cluster-name=new-name")));

    assertThat(configTool("set", "-s", "localhost:" + getNodePort(), "-c", "cluster-name=" + clusterName),
        not(containsOutput("IMPORTANT: A restart of the cluster is required to apply the changes")));
    assertThat(configTool("diagnostic", "-s", "localhost:" + getNodePort(1, 1)),
        allOf(containsOutput("Node restart required: NO"), containsOutput("Node last configuration change details: set cluster-name=" + clusterName)));
  }

  @Test
  public void setNodeMetadataDir() {
    // TDB-5092
    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(), "-c", "metadata-dir=foo"),
        containsOutput("Error: Invalid input: 'metadata-dir=foo'. Reason: Setting 'metadata-dir' cannot be set when node is activated"));
    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.metadata-dir=foo"),
        containsOutput("Error: Invalid input: 'stripe.1.metadata-dir=foo'. Reason: Setting 'metadata-dir' cannot be set when node is activated"));
    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.metadata-dir=foo"),
        containsOutput("Error: Invalid input: 'stripe.1.node.1.metadata-dir=foo'. Reason: Setting 'metadata-dir' cannot be set when node is activated"));
  }

  @Test
  public void testPublicHostPort() {
    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.public-hostname", "-c", "stripe.1.node.1.public-port"),
        allOf(not(containsOutput("stripe.1.node.1.public-hostname=")), not(containsOutput("stripe.1.node.1.public-port="))));

    String publicHostname = "127.0.0.1";
    int publicPort = getNodePort();
    assertThat(configTool("set", "-s", "localhost:" + publicPort, "-c", "stripe.1.node.1.public-hostname=" + publicHostname, "-c", "stripe.1.node.1.public-port=" + publicPort), is(successful()));

    assertThat(
        configTool("get", "-s", publicHostname + ":" + getNodePort(), "-c", "stripe.1.node.1.public-hostname", "-c", "stripe.1.node.1.public-port"),
        allOf(containsOutput("stripe.1.node.1.public-hostname=" + publicHostname), containsOutput("stripe.1.node.1.public-port=" + publicPort)));
  }

  @Test
  public void test_topology_entity_callback_onSettingChange() throws Exception {
    try (DynamicTopologyEntity dynamicTopologyEntity = DynamicTopologyEntityFactory.fetch(
        new TerracottaConnectionService(),
        Collections.singletonList(InetSocketAddress.createUnresolved("localhost", getNodePort())),
        "dynamic-config-topology-entity",
        getConnectionTimeout(),
        new DynamicTopologyEntity.Settings().setRequestTimeout(getConnectionTimeout()),
        null)) {

      CountDownLatch called = new CountDownLatch(1);

      dynamicTopologyEntity.setListener(new DynamicTopologyEntity.Listener() {
        @Override
        public void onSettingChange(Cluster cluster, Configuration configuration) {
          called.countDown();
        }
      });

      assertThat(configTool("set", "-s", "localhost:" + getNodePort(), "-c", "cluster-name=new-name"), is(successful()));

      called.await();
    }
  }
}
