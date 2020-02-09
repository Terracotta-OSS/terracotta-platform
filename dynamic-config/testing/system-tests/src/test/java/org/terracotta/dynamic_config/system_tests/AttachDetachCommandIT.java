/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.system_tests;

import org.junit.Ignore;
import org.junit.Test;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.cli.config_tool.ConfigTool;
import org.terracotta.json.Json;

import java.nio.file.Paths;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.terracotta.testing.ExceptionMatcher.throwing;

/**
 * @author Mathieu Carbou
 */
@ClusterDefinition(stripes = 2, nodesPerStripe = 2, autoStart = false)
public class AttachDetachCommandIT extends DynamicConfigIT {

  //TODO [DYNAMIC-CONFIG]: TDB-4835
  @Test
  @Ignore("//TODO [DYNAMIC-CONFIG]: TDB-4835")
  public void test_attach_to_activated_cluster() throws Exception {
    String destination = "localhost:" + getNodePort();

    // activate a 1x1 cluster
    startNode(1, 1);
    activateCluster();

    // start a second node
    startNode(1, 2);

    assertThat(getUpcomingCluster("localhost", getNodePort()).getNodeCount(), is(equalTo(1)));

    // try forcing the attach
    ConfigTool.start("attach", "-d", destination, "-s", "localhost:" + getNodePort(1, 2));
    assertCommandSuccessful(() -> {
      waitUntil(out::getLog, containsString("Moved to State[ PASSIVE-STANDBY ]"));
    });

    assertThat(getUpcomingCluster("localhost", getNodePort(1, 1)).getNodeCount(), is(equalTo(2)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, 1)).getNodeCount(), is(equalTo(2)));

    assertThat(getUpcomingCluster("localhost", getNodePort(1, 2)).getNodeCount(), is(equalTo(2)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, 2)).getNodeCount(), is(equalTo(2)));
  }

  //TODO [DYNAMIC-CONFIG]: TDB-4835
  @Test
  @Ignore("//TODO [DYNAMIC-CONFIG]: TDB-4835")
  public void test_attach_to_activated_cluster_requiring_restart() throws Exception {
    String destination = "localhost:" + getNodePort();

    // activate a 1x1 cluster
    startNode(1, 1);
    activateCluster();

    // do a change requiring a restart
    ConfigTool.start("set", "-s", destination, "-c", "stripe.1.node.1.tc-properties.foo=bar");
    waitUntil(out::getLog, containsString("IMPORTANT: A restart of the cluster is required to apply the changes"));

    // start a second node
    startNode(1, 2);

    // try to  attach this node to the cluster
    assertThat(
        () -> ConfigTool.start("attach", "-d", destination, "-s", "localhost:" + getNodePort(1, 2)),
        is(throwing(instanceOf(IllegalStateException.class))
            .andMessage(containsString("Impossible to do any topology change. Cluster at address: " + destination + " is waiting to be restarted to apply some pending changes."))));

    // try forcing the attach
    ConfigTool.start("attach", "-f", "-d", destination, "-s", "localhost:" + getNodePort(1, 2));
    assertCommandSuccessful(() -> {
      waitUntil(out::getLog, containsString("Moved to State[ PASSIVE-STANDBY ]"));
    });
  }

  @Test
  public void test_attach_detach_with_unconfigured_nodes() throws Exception {
    startNodes();

    ConfigTool.start("export", "-s", "localhost:" + getNodePort(), "-f", "build/output.json", "-t", "json");
    Cluster cluster = Json.parse(Paths.get("build", "output.json"), Cluster.class);
    assertThat(cluster.getStripes(), hasSize(1));
    assertThat(cluster.getNodeAddresses(), hasSize(1));

    // add a node
    ConfigTool.start("attach", "-d", "localhost:" + getNodePort(), "-s", "localhost:" + getNodePort(1, 2));
    ConfigTool.start("export", "-s", "localhost:" + getNodePort(), "-f", "build/output.json", "-t", "json");
    cluster = Json.parse(Paths.get("build", "output.json"), Cluster.class);
    assertThat(cluster.getStripes(), hasSize(1));
    assertThat(cluster.getNodeAddresses(), hasSize(2));

    // add a stripe
    ConfigTool.start("attach", "-t", "stripe", "-d", "localhost:" + getNodePort(), "-s", "localhost:" + getNodePort(2, 1));
    ConfigTool.start("export", "-s", "localhost:" + getNodePort(), "-f", "build/output.json", "-t", "json");
    cluster = Json.parse(Paths.get("build", "output.json"), Cluster.class);
    assertThat(cluster.getStripes(), hasSize(2));
    assertThat(cluster.getNodeAddresses(), hasSize(3));

    // remove the previously added stripe
    ConfigTool.start("detach", "-t", "stripe", "-d", "localhost:" + getNodePort(), "-s", "localhost:" + getNodePort(2, 1));
    ConfigTool.start("export", "-s", "localhost:" + getNodePort(), "-f", "build/output.json", "-t", "json");
    cluster = Json.parse(Paths.get("build", "output.json"), Cluster.class);
    assertThat(cluster.getStripes(), hasSize(1));
    assertThat(cluster.getNodeAddresses(), hasSize(2));

    // remove the previously added node
    ConfigTool.start("detach", "-d", "localhost:" + getNodePort(), "-s", "localhost:" + getNodePort(1, 2));
    ConfigTool.start("export", "-s", "localhost:" + getNodePort(), "-f", "build/output.json", "-t", "json");
    cluster = Json.parse(Paths.get("build", "output.json"), Cluster.class);
    assertThat(cluster.getStripes(), hasSize(1));
    assertThat(cluster.getNodeAddresses(), hasSize(1));
  }
}
