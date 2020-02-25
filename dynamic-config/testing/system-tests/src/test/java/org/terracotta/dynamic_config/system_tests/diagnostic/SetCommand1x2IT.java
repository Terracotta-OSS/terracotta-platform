/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.system_tests.diagnostic;

import org.junit.Before;
import org.junit.Test;
import org.terracotta.dynamic_config.system_tests.ClusterDefinition;
import org.terracotta.dynamic_config.system_tests.DynamicConfigIT;

import java.io.File;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.terracotta.dynamic_config.system_tests.util.AngelaMatchers.containsOutput;
import static org.terracotta.dynamic_config.system_tests.util.AngelaMatchers.hasExitStatus;
import static org.terracotta.dynamic_config.system_tests.util.AngelaMatchers.successful;

@ClusterDefinition(nodesPerStripe = 2)
public class SetCommand1x2IT extends DynamicConfigIT {

  @Before
  @Override
  public void before() {
    super.before();
    assertThat(configToolInvocation("attach", "-d", "localhost:" + getNodePort(), "-s", "localhost:" + getNodePort(1, 2)), is(successful()));
  }

  /*<--Stripe-wide Tests-->*/
  @Test
  public void testStripe_level_setDataDirectory() {
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.data-dirs.main=stripe1-node1-data-dir"), is(successful()));

    assertThat(configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "data-dirs"),
        allOf(hasExitStatus(0), containsOutput("stripe.1.node.1.data-dirs=main:stripe1-node1-data-dir"), containsOutput("stripe.1.node.2.data-dirs=main:stripe1-node1-data-dir")));
  }

  @Test
  public void testStripe_level_setBackupDirectory() {
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node-backup-dir=backup" + File.separator + "stripe-1"), is(successful()));

    assertThat(configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "node-backup-dir"),
        allOf(hasExitStatus(0), containsOutput("stripe.1.node.1.node-backup-dir=backup" + File.separator + "stripe-1"), containsOutput("stripe.1.node.2.node-backup-dir=backup" + File.separator + "stripe-1")));
  }


  /*<--Cluster-wide Tests-->*/
  @Test
  public void testCluster_setOffheap() {
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.main=1GB"), is(successful()));

    assertThat(configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.main"),
        allOf(hasExitStatus(0), containsOutput("offheap-resources.main=1GB")));
  }

  @Test
  public void testCluster_setBackupDirectory() {
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "node-backup-dir=backup" + File.separator + "data"), is(successful()));

    assertThat(configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "node-backup-dir"),
        allOf(hasExitStatus(0), containsOutput("node-backup-dir=backup" + File.separator + "data")));
  }

  @Test
  public void testCluster_setClientLeaseTime() {
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "client-lease-duration=10s"), is(successful()));

    assertThat(configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "client-lease-duration"),
        allOf(hasExitStatus(0), containsOutput("client-lease-duration=10s")));
  }

  @Test
  public void testCluster_setFailoverPriorityAvailability() {
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "failover-priority=availability"), is(successful()));

    assertThat(configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "failover-priority"),
        allOf(hasExitStatus(0), containsOutput("failover-priority=availability")));
  }

  @Test
  public void testCluster_setFailoverPriorityConsistency() {
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "failover-priority=consistency:2"), is(successful()));

    assertThat(configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "failover-priority"),
        allOf(hasExitStatus(0), containsOutput("failover-priority=consistency:2")));
  }

  @Test
  public void testCluster_setClientReconnectWindow() {
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "client-reconnect-window=10s"), is(successful()));

    assertThat(configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "client-reconnect-window"),
        allOf(hasExitStatus(0), containsOutput("client-reconnect-window=10s")));
  }
}
