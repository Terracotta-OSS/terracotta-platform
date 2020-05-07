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
package org.terracotta.dynamic_config.api.model.nomad;

import org.junit.Test;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Configuration;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.Operation;
import org.terracotta.dynamic_config.api.model.Stripe;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.terracotta.dynamic_config.api.model.Setting.CLUSTER_NAME;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_BACKUP_DIR;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_LOG_DIR;
import static org.terracotta.dynamic_config.api.model.Setting.OFFHEAP_RESOURCES;
import static org.terracotta.dynamic_config.api.model.nomad.Applicability.cluster;
import static org.terracotta.dynamic_config.api.model.nomad.Applicability.node;
import static org.terracotta.dynamic_config.api.model.nomad.Applicability.stripe;
import static org.terracotta.dynamic_config.api.model.nomad.SettingNomadChange.fromConfiguration;
import static org.terracotta.dynamic_config.api.model.nomad.SettingNomadChange.set;
import static org.terracotta.dynamic_config.api.model.nomad.SettingNomadChange.unset;
import static org.terracotta.testing.ExceptionMatcher.throwing;

/**
 * @author Mathieu Carbou
 */
public class SettingNomadChangeTest {

  @Test
  public void test_getSummary() {
    assertThat(set(cluster(), CLUSTER_NAME, "my-cluster").getSummary(), is(equalTo("set cluster-name=my-cluster")));
    assertThat(set(cluster(), OFFHEAP_RESOURCES, "main", "1GB").getSummary(), is(equalTo("set offheap-resources.main=1GB")));
    assertThat(unset(cluster(), OFFHEAP_RESOURCES).getSummary(), is(equalTo("unset offheap-resources")));
    assertThat(unset(cluster(), OFFHEAP_RESOURCES, "main").getSummary(), is(equalTo("unset offheap-resources.main")));
    assertThat(set(stripe(1), NODE_LOG_DIR, "foo").getSummary(), is(equalTo("set log-dir=foo (stripe ID: 1)")));
    assertThat(set(node(1, "node1"), NODE_LOG_DIR, "foo").getSummary(), is(equalTo("set log-dir=foo (stripe ID: 1, node: node1)")));
  }

  @Test
  public void test_fromConfiguration() {
    Configuration configuration = Configuration.valueOf("offheap-resources.main=1GB");
    SettingNomadChange change = fromConfiguration(configuration, Operation.SET, Cluster.newDefaultCluster());
    assertThat(change.getApplicability(), is(equalTo(cluster())));
    assertThat(change.getSetting(), is(equalTo(OFFHEAP_RESOURCES)));

    configuration = Configuration.valueOf("stripe.1.node.1.log-dir=foo");
    change = fromConfiguration(configuration, Operation.SET, Cluster.newDefaultCluster(new Stripe(Node.newDefaultNode("node1", "localhost"))));
    assertThat(change.getApplicability(), is(equalTo(node(1, "node1"))));
    assertThat(change.getSetting(), is(equalTo(NODE_LOG_DIR)));

    configuration = Configuration.valueOf("stripe.1.log-dir=foo");
    change = fromConfiguration(configuration, Operation.SET, Cluster.newDefaultCluster(new Stripe(Node.newDefaultNode("node1", "localhost"))));
    assertThat(change.getApplicability(), is(equalTo(stripe(1))));
    assertThat(change.getSetting(), is(equalTo(NODE_LOG_DIR)));
    assertThat(change.getOperation(), is(equalTo(Operation.SET)));

    configuration = Configuration.valueOf("stripe.1.backup-dir=foo");
    change = fromConfiguration(configuration, Operation.UNSET, Cluster.newDefaultCluster(new Stripe(Node.newDefaultNode("node1", "localhost"))));
    assertThat(change.getApplicability(), is(equalTo(stripe(1))));
    assertThat(change.getSetting(), is(equalTo(NODE_BACKUP_DIR)));
    assertThat(change.getOperation(), is(equalTo(Operation.UNSET)));

    assertThat(
        () -> fromConfiguration(Configuration.valueOf("backup-dir"), Operation.CONFIG, Cluster.newDefaultCluster()),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Operation config cannot be converted to a Nomad change for an active cluster")))));
    assertThat(
        () -> fromConfiguration(Configuration.valueOf("backup-dir"), Operation.GET, Cluster.newDefaultCluster()),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Operation get cannot be converted to a Nomad change for an active cluster")))));
  }

  @Test
  public void test_toConfiguration() {
    Cluster cluster = Cluster.newDefaultCluster(new Stripe(Node.newDefaultNode("node1", "localhost")));

    assertThat(set(cluster(), CLUSTER_NAME, "my-cluster").toConfiguration(cluster), is(equalTo(Configuration.valueOf("cluster-name=my-cluster"))));
    assertThat(unset(cluster(), NODE_BACKUP_DIR).toConfiguration(cluster), is(equalTo(Configuration.valueOf("backup-dir"))));
    assertThat(set(cluster(), NODE_BACKUP_DIR, "foo").toConfiguration(cluster), is(equalTo(Configuration.valueOf("backup-dir=foo"))));
    assertThat(set(stripe(1), NODE_BACKUP_DIR, "foo").toConfiguration(cluster), is(equalTo(Configuration.valueOf("stripe.1.backup-dir=foo"))));
    assertThat(set(node(1, "node1"), NODE_BACKUP_DIR, "foo").toConfiguration(cluster), is(equalTo(Configuration.valueOf("stripe.1.node.1.backup-dir=foo"))));
    assertThat(set(cluster(), OFFHEAP_RESOURCES, "main", "1GB").toConfiguration(cluster), is(equalTo(Configuration.valueOf("offheap-resources.main=1GB"))));
    assertThat(unset(cluster(), OFFHEAP_RESOURCES, "main").toConfiguration(cluster), is(equalTo(Configuration.valueOf("offheap-resources.main"))));

    assertThat(
        () -> set(node(0, "node1"), NODE_BACKUP_DIR, "foo").toConfiguration(cluster),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Invalid stripe ID: 0")))));
    assertThat(
        () -> set(node(1, "node2"), NODE_BACKUP_DIR, "foo").toConfiguration(cluster),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(startsWith("Node: node2 in stripe ID: 1 not found in cluster: Cluster 'null' ( ( node1@localhost:9410 ) )")))));
    assertThat(
        () -> set(node(2, "node1"), NODE_BACKUP_DIR, "foo").toConfiguration(cluster),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(startsWith("Node: node1 in stripe ID: 2 not found in cluster: Cluster 'null' ( ( node1@localhost:9410 ) )")))));
    assertThat(
        () -> set(stripe(0), NODE_BACKUP_DIR, "foo").toConfiguration(cluster),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(startsWith("Invalid stripe ID: 0")))));
    assertThat(
        () -> set(stripe(2), NODE_BACKUP_DIR, "foo").toConfiguration(cluster),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(startsWith("Stripe ID: 2 not found in cluster: Cluster 'null' ( ( node1@localhost:9410 ) )")))));
  }
}