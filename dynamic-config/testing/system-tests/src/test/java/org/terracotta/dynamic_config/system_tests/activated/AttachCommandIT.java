/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.system_tests.activated;

import org.junit.Test;
import org.terracotta.dynamic_config.system_tests.ClusterDefinition;
import org.terracotta.dynamic_config.system_tests.DynamicConfigIT;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.terracotta.dynamic_config.system_tests.util.AngelaMatchers.containsOutput;

/**
 * @author Mathieu Carbou
 */
@ClusterDefinition(nodesPerStripe = 2, autoStart = false)
public class AttachCommandIT extends DynamicConfigIT {

  @Test
  public void test_attach_to_activated_cluster() throws Exception {
    // activate a 1x1 cluster
    out.clearLog();
    startNode(1, 1);
    waitUntil(out::getLog, containsString("Started the server in diagnostic mode"));
    activateCluster();
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 1)).getNodeCount(), is(equalTo(1)));

    // start a second node
    out.clearLog();
    startNode(1, 2);
    waitUntil(out::getLog, containsString("Started the server in diagnostic mode"));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 2)).getNodeCount(), is(equalTo(1)));

    // attach
    configToolInvocation("attach", "-d", "localhost:" + getNodePort(1, 1), "-s", "localhost:" + getNodePort(1, 2));
    assertCommandSuccessful(() -> waitUntil(out::getLog, containsString("Moved to State[ PASSIVE-STANDBY ]")));

    assertThat(getUpcomingCluster("localhost", getNodePort(1, 1)).getNodeCount(), is(equalTo(2)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, 1)).getNodeCount(), is(equalTo(2)));

    assertThat(getUpcomingCluster("localhost", getNodePort(1, 2)).getNodeCount(), is(equalTo(2)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, 2)).getNodeCount(), is(equalTo(2)));
  }

  @Test
  public void test_attach_to_activated_cluster_requiring_restart() throws Exception {
    String destination = "localhost:" + getNodePort();

    // activate a 1x1 cluster
    startNode(1, 1);
    activateCluster();

    // do a change requiring a restart
    configToolInvocation("set", "-s", destination, "-c", "stripe.1.node.1.tc-properties.foo=bar");
    waitUntil(out::getLog, containsString("IMPORTANT: A restart of the cluster is required to apply the changes"));

    // start a second node
    startNode(1, 2);

    // try to attach this node to the cluster
    assertThat(
        configToolInvocation("attach", "-d", destination, "-s", "localhost:" + getNodePort(1, 2)),
        containsOutput("Impossible to do any topology change. Cluster at address: " + destination + " is waiting to be restarted to apply some pending changes. " +
            "You can run the command with -f option to force the comment but at the risk of breaking this cluster configuration consistency. " +
            "The newly added node will be restarted, but not the existing ones."));

    // try forcing the attach
    configToolInvocation("attach", "-f", "-d", destination, "-s", "localhost:" + getNodePort(1, 2));
    assertCommandSuccessful(() -> waitUntil(out::getLog, containsString("Moved to State[ PASSIVE-STANDBY ]")));

    assertThat(getUpcomingCluster("localhost", getNodePort(1, 1)).getNodeCount(), is(equalTo(2)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, 1)).getNodeCount(), is(equalTo(2)));

    assertThat(getUpcomingCluster("localhost", getNodePort(1, 2)).getNodeCount(), is(equalTo(2)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, 2)).getNodeCount(), is(equalTo(2)));
  }
}
