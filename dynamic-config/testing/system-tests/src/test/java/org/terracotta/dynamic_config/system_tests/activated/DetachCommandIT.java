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
@ClusterDefinition(nodesPerStripe = 2, autoActivate = true)
public class DetachCommandIT extends DynamicConfigIT {

  @Test
  public void test_detach_from_activated_cluster() throws Exception {
    // detach
    out.clearLog();
    configToolInvocation("detach", "-d", "localhost:" + getNodePort(1, 1), "-s", "localhost:" + getNodePort(1, 2));
    waitUntil(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));

    assertThat(getUpcomingCluster("localhost", getNodePort(1, 1)).getNodeCount(), is(equalTo(1)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, 1)).getNodeCount(), is(equalTo(1)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, 1)).getSingleNode().get().getNodePort(), is(equalTo(getNodePort(1, 1))));

    assertThat(getUpcomingCluster("localhost", getNodePort(1, 2)).getNodeCount(), is(equalTo(1)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, 2)).getNodeCount(), is(equalTo(1)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, 2)).getSingleNode().get().getNodePort(), is(equalTo(getNodePort(1, 2))));
  }

  @Test
  public void test_detach_from_activated_cluster_requiring_restart() throws Exception {
    String destination = "localhost:" + getNodePort();

    // do a change requiring a restart
    configToolInvocation("set", "-s", destination, "-c", "stripe.1.node.1.tc-properties.foo=bar");
    waitUntil(out::getLog, containsString("IMPORTANT: A restart of the cluster is required to apply the changes"));

    // try to detach this node
    assertThat(
        configToolInvocation("detach", "-d", destination, "-s", "localhost:" + getNodePort(1, 2)),
        containsOutput("Impossible to do any topology change. Cluster at address: " + destination + " is waiting to be restarted to apply some pending changes. " +
            "You can run the command with -f option to force the comment but at the risk of breaking this cluster configuration consistency. " +
            "The newly added node will be restarted, but not the existing ones."));

    // try forcing the attach
    out.clearLog();
    configToolInvocation("detach", "-f", "-d", destination, "-s", "localhost:" + getNodePort(1, 2));
    assertCommandSuccessful(() -> waitUntil(out::getLog, containsString("Moved to State[ PASSIVE-STANDBY ]")));

    assertThat(getUpcomingCluster("localhost", getNodePort(1, 1)).getNodeCount(), is(equalTo(1)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, 1)).getNodeCount(), is(equalTo(1)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, 1)).getSingleNode().get().getNodePort(), is(equalTo(getNodePort(1, 1))));

    assertThat(getUpcomingCluster("localhost", getNodePort(1, 2)).getNodeCount(), is(equalTo(1)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, 2)).getNodeCount(), is(equalTo(1)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, 2)).getSingleNode().get().getNodePort(), is(equalTo(getNodePort(1, 2))));
  }
}
