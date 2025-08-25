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
package org.terracotta.dynamic_config.api.model.nomad;

import org.junit.Test;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Configuration;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.Operation;
import org.terracotta.dynamic_config.api.model.RawPath;
import org.terracotta.dynamic_config.api.model.Testing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.terracotta.dynamic_config.api.model.Operation.IMPORT;
import static org.terracotta.dynamic_config.api.model.Setting.CLUSTER_NAME;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_BACKUP_DIR;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_LOG_DIR;
import static org.terracotta.dynamic_config.api.model.Setting.OFFHEAP_RESOURCES;
import static org.terracotta.dynamic_config.api.model.Testing.N_UIDS;
import static org.terracotta.dynamic_config.api.model.Testing.S_UIDS;
import static org.terracotta.dynamic_config.api.model.Testing.newTestCluster;
import static org.terracotta.dynamic_config.api.model.Testing.newTestNode;
import static org.terracotta.dynamic_config.api.model.Testing.newTestStripe;
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
    assertThat(set(stripe(Testing.A_UID), NODE_LOG_DIR, "foo").getSummary(), is(equalTo("set log-dir=foo (on stripe UID: YLQguzhRSdS6y5M9vnA5mw)")));
    assertThat(set(node(Testing.A_UID), NODE_LOG_DIR, "foo").getSummary(), is(equalTo("set log-dir=foo (on node UID: YLQguzhRSdS6y5M9vnA5mw)")));
  }

  @Test
  public void test_fromConfiguration() {
    Configuration configuration = Configuration.valueOf("offheap-resources.main=1GB");
    SettingNomadChange change = fromConfiguration(configuration, Operation.SET, Testing.newTestCluster());
    assertThat(change.getApplicability(), is(equalTo(cluster())));
    assertThat(change.getSetting(), is(equalTo(OFFHEAP_RESOURCES)));

    configuration = Configuration.valueOf("stripe.1.node.1.log-dir=foo");
    change = fromConfiguration(configuration, Operation.SET, Testing.newTestCluster(Testing.newTestStripe("stripe-1").addNode(Testing.newTestNode("node1", "localhost"))));
    assertThat(change.getApplicability(), is(equalTo(node(Testing.N_UIDS[1]))));
    assertThat(change.getSetting(), is(equalTo(NODE_LOG_DIR)));

    configuration = Configuration.valueOf("stripe.1.log-dir=foo");
    change = fromConfiguration(configuration, Operation.SET, Testing.newTestCluster(Testing.newTestStripe("stripe-1").addNode(Testing.newTestNode("node1", "localhost"))));
    assertThat(change.getApplicability(), is(equalTo(stripe(Testing.S_UIDS[1]))));
    assertThat(change.getSetting(), is(equalTo(NODE_LOG_DIR)));
    assertThat(change.getOperation(), is(equalTo(Operation.SET)));

    configuration = Configuration.valueOf("stripe.1.backup-dir=foo");
    change = fromConfiguration(configuration, Operation.UNSET, Testing.newTestCluster(Testing.newTestStripe("stripe-1").addNode(Testing.newTestNode("node1", "localhost"))));
    assertThat(change.getApplicability(), is(equalTo(stripe(Testing.S_UIDS[1]))));
    assertThat(change.getSetting(), is(equalTo(NODE_BACKUP_DIR)));
    assertThat(change.getOperation(), is(equalTo(Operation.UNSET)));

    assertThat(
        () -> fromConfiguration(Configuration.valueOf("backup-dir"), IMPORT, Testing.newTestCluster()),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Operation import cannot be converted to a Nomad change for an active cluster")))));
    assertThat(
        () -> fromConfiguration(Configuration.valueOf("backup-dir"), Operation.GET, Testing.newTestCluster()),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Operation get cannot be converted to a Nomad change for an active cluster")))));
  }

  @Test
  public void test_toConfiguration() {
    Cluster cluster = Testing.newTestCluster(Testing.newTestStripe("stripe-1").addNode(Testing.newTestNode("node1", "localhost")));

    assertThat(set(cluster(), CLUSTER_NAME, "my-cluster").toConfiguration(cluster), is(equalTo(Configuration.valueOf("cluster-name=my-cluster"))));
    assertThat(unset(cluster(), NODE_BACKUP_DIR).toConfiguration(cluster), is(equalTo(Configuration.valueOf("backup-dir"))));
    assertThat(set(cluster(), NODE_BACKUP_DIR, "foo").toConfiguration(cluster), is(equalTo(Configuration.valueOf("backup-dir=foo"))));
    assertThat(set(stripe(Testing.S_UIDS[1]), NODE_BACKUP_DIR, "foo").toConfiguration(cluster), is(equalTo(Configuration.valueOf("stripe.1.backup-dir=foo"))));
    assertThat(set(node(Testing.N_UIDS[1]), NODE_BACKUP_DIR, "foo").toConfiguration(cluster), is(equalTo(Configuration.valueOf("stripe.1.node.1.backup-dir=foo"))));
    assertThat(set(cluster(), OFFHEAP_RESOURCES, "main", "1GB").toConfiguration(cluster), is(equalTo(Configuration.valueOf("offheap-resources.main=1GB"))));
    assertThat(unset(cluster(), OFFHEAP_RESOURCES, "main").toConfiguration(cluster), is(equalTo(Configuration.valueOf("offheap-resources.main"))));

    assertThat(
        () -> set(node(Testing.A_UID), NODE_BACKUP_DIR, "foo").toConfiguration(cluster),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Stripe not found in cluster: <no name> ( stripe-1:5Zv3uphiRLavoGZthy7JNg ( node1@localhost:9410 ) ) with applicability: node UID: YLQguzhRSdS6y5M9vnA5mw")))));
    assertThat(
        () -> set(stripe(Testing.A_UID), NODE_BACKUP_DIR, "foo").toConfiguration(cluster),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(startsWith("Stripe not found in cluster: <no name> ( stripe-1:5Zv3uphiRLavoGZthy7JNg ( node1@localhost:9410 ) ) with applicability: stripe UID: YLQguzhRSdS6y5M9vnA5mw")))));
  }

  @Test
  public void test_set_unset_backup_dir() {
    Cluster cluster1 = newTestCluster("cluster1", newTestStripe("stripe-1", S_UIDS[1])
        .addNode(newTestNode("node1", "localhost", 9410, N_UIDS[1]))
        .addNode(newTestNode("node2", "localhost", 9410, N_UIDS[2])));

    Cluster cluster2 = newTestCluster("cluster2", newTestStripe("stripe-1", S_UIDS[2])
        .addNode(newTestNode("node1", "localhost", 9410, N_UIDS[1])
            .setBackupDir(RawPath.valueOf("dir")))
        .addNode(newTestNode("node2", "localhost", 9410, N_UIDS[2])
            .setBackupDir(RawPath.valueOf("dir"))));


    // set command executed (for apply and after commit) on cluster1 / node1 to set backup dir of cluster1 / node1
    // => we should apply the change at runtime and update the runtime topology after commit
    assertTrue(set(node(N_UIDS[1]), NODE_BACKUP_DIR, "newDir").canUpdateRuntimeTopology(new NodeContext(cluster1, N_UIDS[1])));

    // set command executed (after commit only) on cluster1 / node2 to set backup dir of cluster1 / node1
    // => we should update the runtime topology after commit (but change won't be applied since not on the right node)
    assertTrue(set(node(N_UIDS[1]), NODE_BACKUP_DIR, "newDir").canUpdateRuntimeTopology(new NodeContext(cluster1, N_UIDS[2])));

    // set command executed (for apply and after commit) on cluster2 / node1 to update backup dir of cluster2 / node1
    // => we should not apply the change at runtime, and we should not update the runtime topology after commit
    // => because we need a restart!
    assertFalse(set(node(N_UIDS[1]), NODE_BACKUP_DIR, "newDir").canUpdateRuntimeTopology(new NodeContext(cluster2, N_UIDS[1])));

    // set command executed (after commit only) on cluster2 / node2 to update backup dir of cluster2 / node1
    // => we should update the runtime topology after commit (but change won't be applied since not on the right node)
    assertTrue(set(node(N_UIDS[1]), NODE_BACKUP_DIR, "newDir").canUpdateRuntimeTopology(new NodeContext(cluster2, N_UIDS[2])));

    // unset command executed (for apply and after commit) on cluster2 / node1 to update backup dir of cluster2 / node1
    // => we should not apply the change at runtime, and we should not update the runtime topology after commit
    // => because we need a restart!
    assertFalse(unset(node(N_UIDS[1]), NODE_BACKUP_DIR).canUpdateRuntimeTopology(new NodeContext(cluster2, N_UIDS[1])));

    // unset command executed (after commit only) on cluster2 / node2 to update backup dir of cluster2 / node1
    // => we should update the runtime topology after commit (but change won't be applied since not on the right node)
    assertTrue(unset(node(N_UIDS[1]), NODE_BACKUP_DIR).canUpdateRuntimeTopology(new NodeContext(cluster2, N_UIDS[2])));
  }

  @Test
  public void test_set_unset_backup_dir_at_cluster_level() {
    Cluster cluster1 = newTestCluster("cluster1", newTestStripe("stripe-1", S_UIDS[1])
        .addNode(newTestNode("node1", "localhost", 9410, N_UIDS[1]))
        .addNode(newTestNode("node2", "localhost", 9410, N_UIDS[2])));

    Cluster cluster2 = newTestCluster("cluster2", newTestStripe("stripe-1", S_UIDS[2])
        .addNode(newTestNode("node1", "localhost", 9410, N_UIDS[1])
            .setBackupDir(RawPath.valueOf("dir")))
        .addNode(newTestNode("node2", "localhost", 9410, N_UIDS[2])
            .setBackupDir(RawPath.valueOf("dir"))));


    assertTrue(set(cluster(), NODE_BACKUP_DIR, "newDir").canUpdateRuntimeTopology(new NodeContext(cluster1, N_UIDS[1])));
    assertTrue(set(cluster(), NODE_BACKUP_DIR, "newDir").canUpdateRuntimeTopology(new NodeContext(cluster1, N_UIDS[2])));

    assertFalse(set(cluster(), NODE_BACKUP_DIR, "newDir").canUpdateRuntimeTopology(new NodeContext(cluster2, N_UIDS[1])));
    assertFalse(set(cluster(), NODE_BACKUP_DIR, "newDir").canUpdateRuntimeTopology(new NodeContext(cluster2, N_UIDS[2])));

    assertFalse(unset(cluster(), NODE_BACKUP_DIR).canUpdateRuntimeTopology(new NodeContext(cluster2, N_UIDS[1])));
    assertFalse(unset(cluster(), NODE_BACKUP_DIR).canUpdateRuntimeTopology(new NodeContext(cluster2, N_UIDS[2])));
  }

  @Test
  public void test_set_log_dir() {
    Cluster cluster2 = newTestCluster("cluster2", newTestStripe("stripe-1", S_UIDS[2])
        .addNode(newTestNode("node1", "localhost", 9410, N_UIDS[1])
            .setLogDir(RawPath.valueOf("dir")))
        .addNode(newTestNode("node2", "localhost", 9410, N_UIDS[2])
            .setLogDir(RawPath.valueOf("dir"))));

    // set command executed (for apply and after commit) on cluster2 / node1 to update log dir of cluster2 / node1
    // => we should not apply the change at runtime, and we should not update the runtime topology after commit
    // => because we need a restart!
    assertFalse(set(node(N_UIDS[1]), NODE_LOG_DIR, "newDir").canUpdateRuntimeTopology(new NodeContext(cluster2, N_UIDS[1])));

    // set command executed (after commit only) on cluster2 / node2 to update log dir of cluster2 / node1
    // => we should update the runtime topology after commit (but change won't be applied since not on the right node)
    assertTrue(set(node(N_UIDS[1]), NODE_LOG_DIR, "newDir").canUpdateRuntimeTopology(new NodeContext(cluster2, N_UIDS[2])));
  }
}