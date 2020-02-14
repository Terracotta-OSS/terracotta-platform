/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.system_tests;

import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.hamcrest.Matchers.containsString;

@ClusterDefinition(nodesPerStripe = 2)
public class SetCommandIT extends DynamicConfigIT {

  @Before
  @Override
  public void before() {
    super.before();
    configToolInvocation("attach", "-d", "localhost:" + getNodePort(), "-s", "localhost:" + getNodePort(1, 2));
    assertCommandSuccessful();
  }

  /*<--Stripe-wide Tests-->*/
  @Test
  public void testStripe_level_setDataDirectory() {
    configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.data-dirs.main=stripe1-node1-data-dir");
    assertCommandSuccessful();

    configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "data-dirs");
    waitUntil(out::getLog, containsString("stripe.1.node.1.data-dirs=main:stripe1-node1-data-dir"));
    waitUntil(out::getLog, containsString("stripe.1.node.2.data-dirs=main:stripe1-node1-data-dir"));
  }

  @Test
  public void testStripe_level_setBackupDirectory() {
    configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node-backup-dir=backup" + File.separator + "stripe-1");
    assertCommandSuccessful();

    configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "node-backup-dir");
    waitUntil(out::getLog, containsString("stripe.1.node.1.node-backup-dir=backup" + File.separator + "stripe-1"));
    waitUntil(out::getLog, containsString("stripe.1.node.2.node-backup-dir=backup" + File.separator + "stripe-1"));
  }


  /*<--Cluster-wide Tests-->*/
  @Test
  public void testCluster_setOffheap() {
    configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.main=1GB");
    assertCommandSuccessful();

    configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.main");
    waitUntil(out::getLog, containsString("offheap-resources.main=1GB"));
  }

  @Test
  public void testCluster_setBackupDirectory() {
    configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "node-backup-dir=backup" + File.separator + "data");
    assertCommandSuccessful();

    configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "node-backup-dir");
    waitUntil(out::getLog, containsString("node-backup-dir=backup" + File.separator + "data"));
  }

  @Test
  public void testCluster_setClientLeaseTime() {
    configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "client-lease-duration=10s");
    assertCommandSuccessful();

    configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "client-lease-duration");
    waitUntil(out::getLog, containsString("client-lease-duration=10s"));
  }

  @Test
  public void testCluster_setFailoverPriorityAvailability() {
    configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "failover-priority=availability");
    assertCommandSuccessful();

    configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "failover-priority");
    waitUntil(out::getLog, containsString("failover-priority=availability"));
  }

  @Test
  public void testCluster_setFailoverPriorityConsistency() {
    configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "failover-priority=consistency:2");
    assertCommandSuccessful();

    configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "failover-priority");
    waitUntil(out::getLog, containsString("failover-priority=consistency:2"));
  }

  @Test
  public void testCluster_setClientReconnectWindow() {
    configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "client-reconnect-window=10s");
    assertCommandSuccessful();

    configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "client-reconnect-window");
    waitUntil(out::getLog, containsString("client-reconnect-window=10s"));
  }

  @Test
  public void testCluster_setClientReconnectWindow_postActivation() throws Exception {
    activateCluster();

    configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "client-reconnect-window=10s");
    assertCommandSuccessful();

    configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "client-reconnect-window");
    waitUntil(out::getLog, containsString("client-reconnect-window=10s"));
  }
}
