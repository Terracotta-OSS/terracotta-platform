/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.api.model.nomad;

import com.terracottatech.dynamic_config.api.model.Cluster;
import com.terracottatech.dynamic_config.api.model.Configuration;
import com.terracottatech.dynamic_config.api.model.Node;
import com.terracottatech.dynamic_config.api.model.Operation;
import com.terracottatech.dynamic_config.api.model.Stripe;
import org.junit.Test;

import static com.terracottatech.dynamic_config.api.model.Setting.CLUSTER_NAME;
import static com.terracottatech.dynamic_config.api.model.Setting.NODE_BACKUP_DIR;
import static com.terracottatech.dynamic_config.api.model.Setting.NODE_LOG_DIR;
import static com.terracottatech.dynamic_config.api.model.Setting.OFFHEAP_RESOURCES;
import static com.terracottatech.dynamic_config.api.model.nomad.Applicability.cluster;
import static com.terracottatech.dynamic_config.api.model.nomad.Applicability.node;
import static com.terracottatech.dynamic_config.api.model.nomad.Applicability.stripe;
import static com.terracottatech.dynamic_config.api.model.nomad.SettingNomadChange.fromConfiguration;
import static com.terracottatech.dynamic_config.api.model.nomad.SettingNomadChange.set;
import static com.terracottatech.dynamic_config.api.model.nomad.SettingNomadChange.unset;
import static com.terracottatech.testing.ExceptionMatcher.throwing;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

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
    assertThat(set(stripe(1), NODE_LOG_DIR, "foo").getSummary(), is(equalTo("set node-log-dir=foo (stripe ID: 1)")));
    assertThat(set(node(1, "node1"), NODE_LOG_DIR, "foo").getSummary(), is(equalTo("set node-log-dir=foo (stripe ID: 1, node: node1)")));
  }

  @Test
  public void test_fromConfiguration() {
    Configuration configuration = Configuration.valueOf("offheap-resources.main=1GB");
    SettingNomadChange change = fromConfiguration(configuration, Operation.SET, new Cluster());
    assertThat(change.getApplicability(), is(equalTo(cluster())));
    assertThat(change.getSetting(), is(equalTo(OFFHEAP_RESOURCES)));

    configuration = Configuration.valueOf("stripe.1.node.1.node-log-dir=foo");
    change = fromConfiguration(configuration, Operation.SET, new Cluster(new Stripe(Node.newDefaultNode("node1", "localhost"))));
    assertThat(change.getApplicability(), is(equalTo(node(1, "node1"))));
    assertThat(change.getSetting(), is(equalTo(NODE_LOG_DIR)));

    configuration = Configuration.valueOf("stripe.1.node-log-dir=foo");
    change = fromConfiguration(configuration, Operation.SET, new Cluster(new Stripe(Node.newDefaultNode("node1", "localhost"))));
    assertThat(change.getApplicability(), is(equalTo(stripe(1))));
    assertThat(change.getSetting(), is(equalTo(NODE_LOG_DIR)));
    assertThat(change.getOperation(), is(equalTo(Operation.SET)));

    configuration = Configuration.valueOf("stripe.1.node-backup-dir=foo");
    change = fromConfiguration(configuration, Operation.UNSET, new Cluster(new Stripe(Node.newDefaultNode("node1", "localhost"))));
    assertThat(change.getApplicability(), is(equalTo(stripe(1))));
    assertThat(change.getSetting(), is(equalTo(NODE_BACKUP_DIR)));
    assertThat(change.getOperation(), is(equalTo(Operation.UNSET)));

    assertThat(
        () -> fromConfiguration(Configuration.valueOf("node-backup-dir"), Operation.CONFIG, new Cluster()),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Operation config cannot be converted to a Nomad change for an active cluster")))));
    assertThat(
        () -> fromConfiguration(Configuration.valueOf("node-backup-dir"), Operation.GET, new Cluster()),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Operation get cannot be converted to a Nomad change for an active cluster")))));
  }

  @Test
  public void test_toConfiguration() {
    Cluster cluster = new Cluster(new Stripe(Node.newDefaultNode("node1", "localhost")));

    assertThat(set(cluster(), CLUSTER_NAME, "my-cluster").toConfiguration(cluster), is(equalTo(Configuration.valueOf("cluster-name=my-cluster"))));
    assertThat(unset(cluster(), NODE_BACKUP_DIR).toConfiguration(cluster), is(equalTo(Configuration.valueOf("node-backup-dir"))));
    assertThat(set(cluster(), NODE_BACKUP_DIR, "foo").toConfiguration(cluster), is(equalTo(Configuration.valueOf("node-backup-dir=foo"))));
    assertThat(set(stripe(1), NODE_BACKUP_DIR, "foo").toConfiguration(cluster), is(equalTo(Configuration.valueOf("stripe.1.node-backup-dir=foo"))));
    assertThat(set(node(1, "node1"), NODE_BACKUP_DIR, "foo").toConfiguration(cluster), is(equalTo(Configuration.valueOf("stripe.1.node.1.node-backup-dir=foo"))));
    assertThat(set(cluster(), OFFHEAP_RESOURCES, "main", "1GB").toConfiguration(cluster), is(equalTo(Configuration.valueOf("offheap-resources.main=1GB"))));
    assertThat(unset(cluster(), OFFHEAP_RESOURCES, "main").toConfiguration(cluster), is(equalTo(Configuration.valueOf("offheap-resources.main"))));

    assertThat(
        () -> set(node(0, "node1"), NODE_BACKUP_DIR, "foo").toConfiguration(cluster),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Invalid stripe ID: 0")))));
    assertThat(
        () -> set(node(1, "node2"), NODE_BACKUP_DIR, "foo").toConfiguration(cluster),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(startsWith("Node: node2 in stripe ID: 1 not found in cluster: Cluster{")))));
    assertThat(
        () -> set(node(2, "node1"), NODE_BACKUP_DIR, "foo").toConfiguration(cluster),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(startsWith("Node: node1 in stripe ID: 2 not found in cluster: Cluster{")))));
    assertThat(
        () -> set(stripe(0), NODE_BACKUP_DIR, "foo").toConfiguration(cluster),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(startsWith("Invalid stripe ID: 0")))));
    assertThat(
        () -> set(stripe(2), NODE_BACKUP_DIR, "foo").toConfiguration(cluster),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(startsWith("Stripe ID: 2 not found in cluster: Cluster{")))));
  }
}